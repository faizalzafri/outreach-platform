package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.EventUpdateRequest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying Redis cache hit/miss behavior and eviction
 * when reading and mutating events through the REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class CacheBehaviorIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Inject
    private TestRestTemplate restTemplate;

    @Inject
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // TestRestTemplate's underlying RestTemplate is a shared bean across all test methods in
        // this class — guard against stacking a duplicate interceptor. Without a tenant header,
        // TenantEntityListener throws IllegalStateException on persist, which the controller
        // turns into a 500 (same root cause fixed in EventServiceIT/AuditLogIT).
        if (restTemplate.getRestTemplate().getInterceptors().isEmpty()) {
            restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add(TenantConstants.X_TENANT_ID_HEADER, TenantConstants.DEFAULT_TENANT_ID.toString());
                return execution.execute(request, body);
            });
        }
    }

    @Test
    void getEvent_secondCallShouldBeServedFromCache() {
        EventDto created = createEvent("Cache Hit Test");

        // First GET — populates cache
        ResponseEntity<EventDto> first = restTemplate.getForEntity(
                "/events/{id}", EventDto.class, created.id());
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify cache key was written
        Set<String> keys = redisTemplate.keys("eventCache*");
        assertThat(keys).isNotEmpty();

        // Second GET — served from cache (same result, cache entry still present)
        ResponseEntity<EventDto> second = restTemplate.getForEntity(
                "/events/{id}", EventDto.class, created.id());
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().id()).isEqualTo(created.id());
        assertThat(second.getBody().eventName()).isEqualTo(created.eventName());
    }

    @Test
    void updateEvent_shouldEvictCacheEntry() {
        EventDto created = createEvent("Cache Evict Test");

        // Populate cache via GET
        restTemplate.getForEntity("/events/{id}", EventDto.class, created.id());

        // Verify cache contains the entry
        Set<String> keysBefore = redisTemplate.keys("eventCache*");
        assertThat(keysBefore).isNotEmpty();

        // Update event — triggers @CacheEvict
        EventUpdateRequest update = new EventUpdateRequest(
                "Updated Cache Evict Name", null, null, null, null, null, null, null);
        ResponseEntity<EventDto> updateResponse = restTemplate.exchange(
                "/events/{id}", HttpMethod.PUT, new HttpEntity<>(update),
                EventDto.class, created.id());
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Next GET should return fresh data (updated name) — proves cache was evicted
        ResponseEntity<EventDto> fresh = restTemplate.getForEntity(
                "/events/{id}", EventDto.class, created.id());
        assertThat(fresh.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fresh.getBody()).isNotNull();
        assertThat(fresh.getBody().eventName()).isEqualTo("Updated Cache Evict Name");
    }

    @Test
    void getEvent_nonExistingId_shouldNotPopulateCache() {
        UUID nonExistent = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/events/{id}", String.class, nonExistent);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Cache should not contain an entry for missing events (disableCachingNullValues)
        Set<String> keys = redisTemplate.keys("eventCache::" + nonExistent + "*");
        assertThat(keys == null || keys.isEmpty()).isTrue();
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private EventDto createEvent(String name) {
        EventCreateRequest request = new EventCreateRequest(
                name, "Cache test event",
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                "Pune", "Tech Park", "CSR", 20);

        ResponseEntity<EventDto> response = restTemplate.postForEntity("/events", request, EventDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
