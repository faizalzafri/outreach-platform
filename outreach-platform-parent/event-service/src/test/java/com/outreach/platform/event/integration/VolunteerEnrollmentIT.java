package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.event.model.dto.EnrollmentDto;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.VolunteerEnrollRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for volunteer enrollment REST endpoints
 * ({@code /events/{eventId}/volunteers}).
 *
 * <p>Relies on the seeded volunteer profiles (EMP001-EMP015, see
 * {@code 20240102-001-seed-data.sql}) which Liquibase loads into every fresh Testcontainers
 * database the same as production — this suite doesn't need to create its own volunteers since
 * there's no volunteer-create endpoint (volunteers are HR-managed / import-only).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class VolunteerEnrollmentIT {

    static {
        // VolunteerEntity's email/phone/fullName are @PiiField, encrypted via
        // AesEncryptionConverter, which requires this key (env var or, as here, the
        // system-property fallback the converter documents as "for testing") to be set before any
        // VolunteerEntity is read. Unlike TenantScopedRepositoriesIsolationIT (which only
        // encrypts/decrypts its own freshly-created rows within one run, so any key works as long
        // as it's consistent), this suite reads the seeded volunteer rows from
        // 20260914-001-fix-plaintext-seed-pii.sql, which were encrypted ahead of time with this
        // exact fixed dev key — the same one docker-compose sets for event-service — so it must
        // match precisely or decryption fails with AEADBadTagException (tag mismatch).
        System.setProperty("pii.encryption.key", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        if (restTemplate.getRestTemplate().getInterceptors().isEmpty()) {
            restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add(TenantConstants.X_TENANT_ID_HEADER, TenantConstants.DEFAULT_TENANT_ID.toString());
                return execution.execute(request, body);
            });
        }

        EventCreateRequest request = new EventCreateRequest(
                "Enrollment Test Event " + UUID.randomUUID().toString().substring(0, 8),
                "Test event description",
                LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 15),
                "Bangalore", "Test Venue", "CSR", 30);
        ResponseEntity<EventDto> created = restTemplate.postForEntity("/events", request, EventDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        eventId = created.getBody().id();
    }

    @Test
    void enrollVolunteers_validEmployeeId_shouldReturn201AndAppearInList() {
        ResponseEntity<EnrollmentDto[]> response = enroll(eventId, "EMP001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).hasSize(1);
        EnrollmentDto enrollment = response.getBody()[0];
        assertThat(enrollment.employeeId()).isEqualTo("EMP001");
        assertThat(enrollment.eventId()).isEqualTo(eventId);
        assertThat(enrollment.volunteerId()).isNotNull();

        ResponseEntity<EnrollmentDto[]> listResponse = restTemplate.getForEntity(
                "/events/{eventId}/volunteers", EnrollmentDto[].class, eventId);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody()))
                .extracting(EnrollmentDto::employeeId)
                .containsExactly("EMP001");
    }

    @Test
    void enrollVolunteers_multipleEmployeeIds_shouldEnrollAll() {
        ResponseEntity<EnrollmentDto[]> response = enroll(eventId, "EMP002", "EMP003");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void enrollVolunteers_duplicateEnrollment_shouldReturn409() {
        enroll(eventId, "EMP004");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/events/{eventId}/volunteers",
                new VolunteerEnrollRequest(List.of("EMP004")),
                String.class,
                eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void enrollVolunteers_unknownEmployeeId_shouldReturn404() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/events/{eventId}/volunteers",
                new VolunteerEnrollRequest(List.of("NO-SUCH-EMPLOYEE")),
                String.class,
                eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enrollVolunteers_unknownEvent_shouldReturn404() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/events/{eventId}/volunteers",
                new VolunteerEnrollRequest(List.of("EMP005")),
                String.class,
                UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listEnrolledVolunteers_noEnrollments_shouldReturnEmptyList() {
        ResponseEntity<EnrollmentDto[]> response = restTemplate.getForEntity(
                "/events/{eventId}/volunteers", EnrollmentDto[].class, eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void removeVolunteer_afterEnroll_shouldReturn204AndRemoveFromList() {
        enroll(eventId, "EMP006");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/events/{eventId}/volunteers/{employeeId}",
                HttpMethod.DELETE,
                null,
                Void.class,
                eventId, "EMP006");

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<EnrollmentDto[]> listResponse = restTemplate.getForEntity(
                "/events/{eventId}/volunteers", EnrollmentDto[].class, eventId);
        assertThat(listResponse.getBody()).isEmpty();
    }

    @Test
    void removeVolunteer_notEnrolled_shouldReturn404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/events/{eventId}/volunteers/{employeeId}",
                HttpMethod.DELETE,
                null,
                String.class,
                eventId, "EMP007");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<EnrollmentDto[]> enroll(UUID eventId, String... employeeIds) {
        ResponseEntity<EnrollmentDto[]> response = restTemplate.postForEntity(
                "/events/{eventId}/volunteers",
                new VolunteerEnrollRequest(List.of(employeeIds)),
                EnrollmentDto[].class,
                eventId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response;
    }
}
