package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.event.model.VolunteerAvailability;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.VolunteerDto;
import com.outreach.platform.event.model.dto.VolunteerEnrollRequest;
import com.outreach.platform.event.model.dto.VolunteerProfileUpdateRequest;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the volunteer directory REST endpoints ({@code /volunteers}).
 *
 * <p>Relies on the seeded volunteer profiles (EMP001-EMP015, see
 * {@code 20240102-001-seed-data.sql}) which Liquibase loads into every fresh Testcontainers
 * database the same as production — see {@link VolunteerEnrollmentIT} for why the
 * {@code pii.encryption.key} system property below is required and must match the fixed dev key
 * those seed rows were encrypted with.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class VolunteerIT {

    static {
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
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        if (restTemplate.getRestTemplate().getInterceptors().isEmpty()) {
            restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add(TenantConstants.X_TENANT_ID_HEADER, TenantConstants.DEFAULT_TENANT_ID.toString());
                return execution.execute(request, body);
            });
        }
    }

    @Test
    void listVolunteers_noSearch_shouldReturnSeededVolunteers() {
        ResponseEntity<String> response = restTemplate.getForEntity("/volunteers?page=0&size=50", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"employeeId\":\"EMP001\"");
        assertThat(response.getBody()).contains("\"totalElements\":15");
    }

    @Test
    void listVolunteers_withSearch_shouldFilterByBaseLocation() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/volunteers?search=Mumbai&page=0&size=50", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"employeeId\":\"EMP001\"");
        assertThat(response.getBody()).doesNotContain("\"employeeId\":\"EMP003\"");
    }

    @Test
    void getVolunteer_existingEmployeeId_shouldReturnProfile() {
        ResponseEntity<VolunteerDto> response = restTemplate.getForEntity(
                "/volunteers/{employeeId}", VolunteerDto.class, "EMP001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().employeeId()).isEqualTo("EMP001");
        assertThat(response.getBody().fullName()).isEqualTo("Arun Kumar");
        assertThat(response.getBody().baseLocation()).isEqualTo("Mumbai");
    }

    @Test
    void getVolunteer_unknownEmployeeId_shouldReturn404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/volunteers/{employeeId}", String.class, "NO-SUCH-EMPLOYEE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateProfile_changesAvailability_shouldPersist() {
        // Dedicated employeeId so this mutation doesn't affect other tests sharing the container.
        VolunteerProfileUpdateRequest request = new VolunteerProfileUpdateRequest(
                null, null, null, null, null, null, null, VolunteerAvailability.ON_LEAVE);

        ResponseEntity<VolunteerDto> response = restTemplate.exchange(
                "/volunteers/{employeeId}",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                VolunteerDto.class,
                "EMP015");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().availability()).isEqualTo(VolunteerAvailability.ON_LEAVE);
        // Untouched field survives the partial update
        assertThat(response.getBody().employeeId()).isEqualTo("EMP015");

        ResponseEntity<VolunteerDto> refetched = restTemplate.getForEntity(
                "/volunteers/{employeeId}", VolunteerDto.class, "EMP015");
        assertThat(refetched.getBody().availability()).isEqualTo(VolunteerAvailability.ON_LEAVE);
    }

    @Test
    void updateProfile_unknownEmployeeId_shouldReturn404() {
        VolunteerProfileUpdateRequest request = new VolunteerProfileUpdateRequest(
                null, null, null, null, null, null, null, VolunteerAvailability.AVAILABLE);

        ResponseEntity<String> response = restTemplate.exchange(
                "/volunteers/{employeeId}",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                String.class,
                "NO-SUCH-EMPLOYEE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getHistory_afterEnrollment_shouldIncludeTheEvent() {
        EventCreateRequest eventRequest = new EventCreateRequest(
                "History Test Event " + UUID.randomUUID().toString().substring(0, 8),
                "Test event description",
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                "Pune", "Test Venue", "CSR", 30);
        ResponseEntity<EventDto> event = restTemplate.postForEntity("/events", eventRequest, EventDto.class);
        assertThat(event.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID eventId = event.getBody().id();

        ResponseEntity<Void> enrollResponse = restTemplate.postForEntity(
                "/events/{eventId}/volunteers", new VolunteerEnrollRequest(List.of("EMP009")), Void.class, eventId);
        assertThat(enrollResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> history = restTemplate.getForEntity(
                "/volunteers/{employeeId}/history?page=0&size=50", String.class, "EMP009");

        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(history.getBody()).contains(event.getBody().eventName());
        assertThat(history.getBody()).contains("\"attendanceStatus\":\"REGISTERED\"");
    }

    @Test
    void getHistory_unknownEmployeeId_shouldReturn404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/volunteers/{employeeId}/history", String.class, "NO-SUCH-EMPLOYEE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getHistory_noEnrollments_shouldReturnEmptyPage() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/volunteers/{employeeId}/history", String.class, "EMP014");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"empty\":true");
    }
}
