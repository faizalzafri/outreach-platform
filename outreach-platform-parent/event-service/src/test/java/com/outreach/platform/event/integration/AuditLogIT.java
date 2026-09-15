package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.event.model.AuditLogDocument;
import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.StatusTransitionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that mutations write audit log entries to MongoDB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuditLogIT {

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

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        // TestRestTemplate's underlying RestTemplate is a shared bean across all test methods in
        // this class — guard against stacking a duplicate interceptor. Without a tenant header,
        // TenantEntityListener throws IllegalStateException on persist, which the controller
        // turns into a 500 that this suite never noticed because it was never actually run before
        // the failsafe-plugin wiring fix (same root cause as EventServiceIT).
        if (restTemplate.getRestTemplate().getInterceptors().isEmpty()) {
            restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add(TenantConstants.X_TENANT_ID_HEADER, TenantConstants.DEFAULT_TENANT_ID.toString());
                return execution.execute(request, body);
            });
        }
    }

    @Test
    void createEvent_shouldWriteAuditLogEntry() {
        EventDto event = createEvent("Audit Test Event Create");

        // Query MongoDB audit_logs collection for the CREATE_EVENT entry
        List<AuditLogDocument> logs = mongoTemplate.find(
                Query.query(Criteria.where("action").is("CREATE_EVENT")
                        .and("resourceId").is(event.id().toString())),
                AuditLogDocument.class,
                "audit_logs");

        assertThat(logs).isNotEmpty();
        AuditLogDocument auditEntry = logs.get(0);
        assertThat(auditEntry.getAction()).isEqualTo("CREATE_EVENT");
        assertThat(auditEntry.getResourceType()).isEqualTo("Event");
        assertThat(auditEntry.getResourceId()).isEqualTo(event.id().toString());
        assertThat(auditEntry.getTimestamp()).isNotNull();
    }

    @Test
    void statusTransition_shouldWriteAuditLogEntry() {
        EventDto event = createEvent("Audit Test Event Transition");

        // Transition DRAFT → PUBLISHED
        ResponseEntity<EventDto> response = restTemplate.exchange(
                "/events/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(new StatusTransitionRequest(EventStatus.PUBLISHED)),
                EventDto.class,
                event.id());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Query audit logs for status transition
        List<AuditLogDocument> logs = mongoTemplate.find(
                Query.query(Criteria.where("action").is("UPDATE_STATUS")
                        .and("resourceId").is(event.id().toString())),
                AuditLogDocument.class,
                "audit_logs");

        assertThat(logs).isNotEmpty();
        AuditLogDocument auditEntry = logs.get(0);
        assertThat(auditEntry.getAction()).isEqualTo("UPDATE_STATUS");
        assertThat(auditEntry.getResourceType()).isEqualTo("Event");
        assertThat(auditEntry.getResourceId()).isEqualTo(event.id().toString());
        assertThat(auditEntry.getTimestamp()).isNotNull();
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private EventDto createEvent(String name) {
        EventCreateRequest request = new EventCreateRequest(
                name, "Audit test event",
                LocalDate.of(2025, 5, 10), LocalDate.of(2025, 5, 10),
                "Hyderabad", "Conference Hall", "CSR", 25);

        ResponseEntity<EventDto> response = restTemplate.postForEntity("/events", request, EventDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
