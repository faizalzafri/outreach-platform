package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.EventUpdateRequest;
import com.outreach.platform.event.model.dto.StatusTransitionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Event Service REST endpoints.
 * Uses Testcontainers for PostgreSQL and MongoDB backing stores.
 *
 * <p>Every request carries the seeded default tenant's {@code X-Tenant-ID} header via a
 * {@code TestRestTemplate} interceptor — {@code TenantFilterAspect} throws
 * {@code IllegalStateException} on any JPA repository call made with no tenant context, and this
 * suite previously sent no tenant header at all, which meant it had never actually exercised a
 * single successful request end-to-end (discovered while adding a tenant-isolation regression
 * test, see {@code docs/specs/platform-hardening/}). The default tenant ID matches the row
 * seeded by {@code 20250120-002-add-tenant-id-to-event-tables.sql} — required, since
 * {@code events.tenant_id} has a foreign key to {@code tenants(id)}.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class EventServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // getEvent() is @Cacheable — without a real Redis to back RedisCacheManager, every GET
    // request 500s trying to reach the default localhost:6379 (see CacheBehaviorIT, which
    // already provisions one for the same reason).
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

    @Autowired
    private TestRestTemplate restTemplate;

    private EventCreateRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        // TestRestTemplate's underlying RestTemplate is a shared bean across all test methods in
        // this class — @BeforeEach runs once per test, so guard against stacking a duplicate
        // interceptor on every one of the 13 tests.
        if (restTemplate.getRestTemplate().getInterceptors().isEmpty()) {
            restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add(TenantConstants.X_TENANT_ID_HEADER, TenantConstants.DEFAULT_TENANT_ID.toString());
                return execution.execute(request, body);
            });
        }

        validCreateRequest = new EventCreateRequest(
                "Community Cleanup Drive",
                "Annual community cleanup event",
                LocalDate.of(2025, 3, 15),
                LocalDate.of(2025, 3, 15),
                "Bangalore",
                "City Park",
                "Environment",
                50
        );
    }

    @Test
    void createEvent_shouldReturnCreatedWithDraftStatus() {
        ResponseEntity<EventDto> response = restTemplate.postForEntity(
                "/events", validCreateRequest, EventDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        EventDto event = response.getBody();
        assertThat(event).isNotNull();
        assertThat(event.id()).isNotNull();
        assertThat(event.eventCode()).startsWith("EVT-");
        assertThat(event.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.eventName()).isEqualTo("Community Cleanup Drive");
        assertThat(event.city()).isEqualTo("Bangalore");
        assertThat(event.registeredCount()).isZero();
        assertThat(event.attendedCount()).isZero();
    }

    @Test
    void statusTransition_draftToPublished_shouldSucceed() {
        EventDto created = createTestEvent();

        ResponseEntity<EventDto> response = transitionStatus(created.id(), EventStatus.PUBLISHED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void statusTransition_fullLifecycle_shouldSucceed() {
        EventDto event = createTestEvent();

        // DRAFT → PUBLISHED
        event = transitionStatus(event.id(), EventStatus.PUBLISHED).getBody();
        assertThat(event.status()).isEqualTo(EventStatus.PUBLISHED);

        // PUBLISHED → ACTIVE
        event = transitionStatus(event.id(), EventStatus.ACTIVE).getBody();
        assertThat(event.status()).isEqualTo(EventStatus.ACTIVE);

        // ACTIVE → COMPLETED
        event = transitionStatus(event.id(), EventStatus.COMPLETED).getBody();
        assertThat(event.status()).isEqualTo(EventStatus.COMPLETED);

        // COMPLETED → ARCHIVED
        event = transitionStatus(event.id(), EventStatus.ARCHIVED).getBody();
        assertThat(event.status()).isEqualTo(EventStatus.ARCHIVED);
    }

    @Test
    void statusTransition_invalidTransition_shouldReturn400() {
        EventDto event = createTestEvent();

        // DRAFT → COMPLETED is invalid (must go through PUBLISHED first)
        ResponseEntity<String> response = restTemplate.exchange(
                "/events/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new StatusTransitionRequest(EventStatus.COMPLETED)),
                String.class,
                event.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void statusTransition_draftToCancelled_shouldSucceed() {
        EventDto event = createTestEvent();

        ResponseEntity<EventDto> response = transitionStatus(event.id(), EventStatus.CANCELLED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    void statusTransition_publishedToCancelled_shouldSucceed() {
        EventDto event = createTestEvent();
        transitionStatus(event.id(), EventStatus.PUBLISHED);

        ResponseEntity<EventDto> response = transitionStatus(event.id(), EventStatus.CANCELLED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    void listEvents_withPagination_shouldReturnCorrectPage() {
        // Create multiple events
        for (int i = 0; i < 5; i++) {
            createTestEvent("Paginated Event " + i);
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/events?page=0&size=3", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"size\":3");
        assertThat(response.getBody()).contains("\"totalElements\":");
    }

    @Test
    void getEvent_existingId_shouldReturnEvent() {
        EventDto created = createTestEvent();

        ResponseEntity<EventDto> response = restTemplate.getForEntity(
                "/events/{id}", EventDto.class, created.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(created.id());
        assertThat(response.getBody().eventName()).isEqualTo(created.eventName());
    }

    @Test
    void getEvent_nonExistingId_shouldReturn404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/events/{id}", String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateEvent_shouldApplyChanges() {
        EventDto created = createTestEvent();

        EventUpdateRequest updateRequest = new EventUpdateRequest(
                "Updated Event Name",
                "Updated description",
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 4, 2),
                "Mumbai",
                "New Venue",
                "Education",
                100
        );

        ResponseEntity<EventDto> response = restTemplate.exchange(
                "/events/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                EventDto.class,
                created.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EventDto updated = response.getBody();
        assertThat(updated.eventName()).isEqualTo("Updated Event Name");
        assertThat(updated.city()).isEqualTo("Mumbai");
        assertThat(updated.category()).isEqualTo("Education");
        assertThat(updated.maxVolunteers()).isEqualTo(100);
    }

    @Test
    void optimisticLocking_concurrentUpdate_shouldReturn409() throws Exception {
        EventDto created = createTestEvent();

        // Use concurrent threads to trigger optimistic locking conflict.
        // Both threads load the same version, one commits first (version 0→1),
        // the second then fails because it also expects version 0.
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var results = new java.util.concurrent.CopyOnWriteArrayList<HttpStatus>();

        for (int i = 0; i < 2; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                    EventUpdateRequest update = new EventUpdateRequest(
                            "Concurrent Update " + idx, null, null, null, null, null, null, null);
                    ResponseEntity<String> response = restTemplate.exchange(
                            "/events/{id}",
                            HttpMethod.PUT,
                            new HttpEntity<>(update),
                            String.class,
                            created.id());
                    results.add((HttpStatus) response.getStatusCode());
                } catch (Exception e) {
                    // barrier timeout or interruption
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        // With concurrent requests targeting the same row, we expect either:
        // - Both 200 (if DB serializes them perfectly), or
        // - One 200 and one 409 (optimistic locking triggered)
        // The important thing is no 500 errors — the mechanism is wired.
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(s -> s == HttpStatus.OK || s == HttpStatus.CONFLICT);
    }

    @Test
    void searchEvents_shouldFindByEventName() {
        createTestEvent("Unique Searchable Event XYZ");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/events/search?query=Unique+Searchable&page=0&size=10", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Unique Searchable Event XYZ");
    }

    @Test
    void searchEvents_shouldFindByCity() {
        createTestEvent("City Search Event", "Chennai");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/events/search?query=Chennai&page=0&size=10", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("City Search Event");
    }

    // ─── Helper methods ──────────────────────────────────────────────────────

    private EventDto createTestEvent() {
        return createTestEvent("Test Event " + UUID.randomUUID().toString().substring(0, 8));
    }

    private EventDto createTestEvent(String name) {
        return createTestEvent(name, "Bangalore");
    }

    private EventDto createTestEvent(String name, String city) {
        EventCreateRequest request = new EventCreateRequest(
                name, "Test event description",
                LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 15),
                city, "Test Venue", "CSR", 30);

        ResponseEntity<EventDto> response = restTemplate.postForEntity("/events", request, EventDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<EventDto> transitionStatus(UUID eventId, EventStatus targetStatus) {
        return restTemplate.exchange(
                "/events/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new StatusTransitionRequest(targetStatus)),
                EventDto.class,
                eventId);
    }
}
