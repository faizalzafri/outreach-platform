package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.model.AssignmentRole;
import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.dto.EnrollmentDto;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.EventSearchCriteria;
import com.outreach.platform.event.model.dto.FeedbackEligibilityDto;
import com.outreach.platform.event.model.dto.PocAssignRequest;
import com.outreach.platform.event.model.dto.StatusTransitionRequest;
import com.outreach.platform.event.service.EventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Counts that follow enrollments, attendance, locked events, POC scoping, and the lookups
 * feedback-service makes (names, feedback eligibility).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class EventParticipationIT {

    static {
        System.setProperty("pii.encryption.key", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    // Seeded by Liquibase into the default tenant.
    private static final UUID DRAFT_EVENT = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID ACTIVE_EVENT = UUID.fromString("b0000000-0000-0000-0000-000000000005");
    private static final UUID OTHER_ACTIVE_EVENT = UUID.fromString("b0000000-0000-0000-0000-000000000007");
    private static final UUID POC_ONLY_EVENT = UUID.fromString("b0000000-0000-0000-0000-000000000006");
    private static final UUID POC_USER = UUID.fromString("a0000000-0000-0000-0000-000000000005");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // Event details are cached; counts must be evicted when enrollments change.
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

    @Autowired
    private EventService eventService;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().getInterceptors().clear();
        restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set(TenantConstants.X_TENANT_ID_HEADER, TenantConstants.DEFAULT_TENANT_ID.toString());
            return execution.execute(request, body);
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void counts_followEnrollments_includingImports_andAttendance() {
        List<EnrollmentDto> enrolled = enrollments(ACTIVE_EVENT);
        EventDto event = event(ACTIVE_EVENT);
        assertThat(event.registeredCount()).isEqualTo(enrolled.size());
        long attended = enrolled.stream().filter(e -> e.attendanceStatus().name().equals("ATTENDED")).count();
        assertThat(event.attendedCount()).isEqualTo((int) attended);

        restTemplate.postForEntity("/volunteers/import", Map.of(
                "employeeId", "EMP-IMPORT-77", "fullName", "Imported Person", "email", "imported@example.com",
                "eventCode", "EVT-2024-005"), String.class);
        assertThat(event(ACTIVE_EVENT).registeredCount()).isEqualTo(enrolled.size() + 1);

        UUID someone = enrolled.get(0).volunteerId();
        ResponseEntity<String> marked = restTemplate.exchange("/events/{id}/volunteers/attendance", HttpMethod.PUT,
                new HttpEntity<>(Map.of("entries", List.of(Map.of("volunteerId", someone, "status", "NOT_ATTENDED")))),
                String.class, ACTIVE_EVENT);
        assertThat(marked.getStatusCode()).isEqualTo(HttpStatus.OK);
        long attendedNow = enrollments(ACTIVE_EVENT).stream()
                .filter(e -> e.attendanceStatus().name().equals("ATTENDED")).count();
        assertThat(event(ACTIVE_EVENT).attendedCount()).isEqualTo((int) attendedNow);
    }

    @Test
    void attendance_waitsForTheEventToStart_andCancelledEventsAreLocked() {
        ResponseEntity<String> early = restTemplate.exchange("/events/{id}/volunteers/attendance", HttpMethod.PUT,
                new HttpEntity<>(Map.of("entries", List.of(Map.of("volunteerId", UUID.randomUUID(), "status", "ATTENDED")))),
                String.class, DRAFT_EVENT);
        assertThat(early.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        restTemplate.exchange("/events/{id}/status", HttpMethod.PATCH,
                new HttpEntity<>(new StatusTransitionRequest(EventStatus.CANCELLED)), String.class, DRAFT_EVENT);
        ResponseEntity<String> edit = restTemplate.exchange("/events/{id}", HttpMethod.PUT,
                new HttpEntity<>(Map.of("eventName", "Renamed")), String.class, DRAFT_EVENT);
        assertThat(edit.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(edit.getBody()).contains("cancelled event can no longer be edited");
    }

    @Test
    void names_andFeedbackEligibility() {
        Map<UUID, String> names = restTemplate.exchange("/events/names", HttpMethod.POST,
                new HttpEntity<>(List.of(ACTIVE_EVENT, UUID.randomUUID())),
                new ParameterizedTypeReference<Map<UUID, String>>() { }).getBody();
        assertThat(names).containsOnly(Map.entry(ACTIVE_EVENT, "Women Empowerment Seminar"));

        UUID volunteer = enrollments(ACTIVE_EVENT).get(0).volunteerId();
        Map<UUID, String> volunteers = restTemplate.exchange("/volunteers/names", HttpMethod.POST,
                new HttpEntity<>(List.of(volunteer)), new ParameterizedTypeReference<Map<UUID, String>>() { }).getBody();
        assertThat(volunteers.get(volunteer)).isEqualTo(enrollments(ACTIVE_EVENT).get(0).volunteerName());

        FeedbackEligibilityDto before = restTemplate.getForObject(
                "/events/{id}/feedback-eligibility?userId={u}", FeedbackEligibilityDto.class, OTHER_ACTIVE_EVENT, POC_USER);
        assertThat(before.status()).isEqualTo(EventStatus.ACTIVE);
        assertThat(before.assigned()).isFalse();

        restTemplate.postForEntity("/events/{id}/pocs", new PocAssignRequest(POC_USER, AssignmentRole.PRIMARY),
                String.class, OTHER_ACTIVE_EVENT);
        assertThat(restTemplate.getForObject("/events/{id}/feedback-eligibility?userId={u}",
                FeedbackEligibilityDto.class, OTHER_ACTIVE_EVENT, POC_USER).assigned()).isTrue();
    }

    @Test
    void listing_filtersByTextTogetherWithStatus() {
        String active = restTemplate.getForObject("/events?query=women&status=ACTIVE", String.class);
        assertThat(active).contains("Women Empowerment Seminar").doesNotContain("Coastal Cleanup Drive");
        assertThat(restTemplate.getForObject("/events?query=women&status=DRAFT", String.class))
                .doesNotContain("Women Empowerment Seminar");
    }

    @Test
    void aPoc_seesOnlyTheirOwnEvents() {
        restTemplate.postForEntity("/events/{id}/pocs", new PocAssignRequest(POC_USER, AssignmentRole.PRIMARY),
                String.class, POC_ONLY_EVENT);

        TenantContext.setCurrentTenantId(TenantConstants.DEFAULT_TENANT_ID);
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("rahul_verma")
                .claim("uid", POC_USER.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_POC"))));

        List<UUID> listed = eventService.listEvents(new EventSearchCriteria(null, null, null, null, null, null),
                Pageable.ofSize(50)).map(EventDto::id).getContent();
        assertThat(listed).contains(POC_ONLY_EVENT).doesNotContain(ACTIVE_EVENT, DRAFT_EVENT);
    }

    private EventDto event(UUID id) {
        return restTemplate.getForObject("/events/{id}", EventDto.class, id);
    }

    private List<EnrollmentDto> enrollments(UUID eventId) {
        return restTemplate.exchange("/events/{id}/volunteers", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<EnrollmentDto>>() { }, eventId).getBody();
    }
}
