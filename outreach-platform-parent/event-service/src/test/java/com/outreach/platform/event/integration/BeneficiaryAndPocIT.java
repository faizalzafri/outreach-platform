package com.outreach.platform.event.integration;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.event.model.AssignmentRole;
import com.outreach.platform.event.model.dto.BeneficiaryCreateRequest;
import com.outreach.platform.event.model.dto.BeneficiaryDto;
import com.outreach.platform.event.model.dto.BeneficiaryUpdateRequest;
import com.outreach.platform.event.model.dto.PocAssignRequest;
import com.outreach.platform.event.model.dto.PocAssignmentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the beneficiary ({@code /beneficiaries}) and POC assignment
 * ({@code /events/{eventId}/pocs}) endpoints, including tenant isolation of beneficiary reads.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class BeneficiaryAndPocIT {

    static {
        System.setProperty("pii.encryption.key", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    // Seeded by Liquibase (20240102-001-seed-data.sql) into the default tenant.
    private static final UUID SEEDED_DRAFT_EVENT = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    private static final UUID SEEDED_POC_USER = UUID.fromString("a0000000-0000-0000-0000-000000000004");

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

    /** Tenant sent on every request; tests switch it to simulate another tenant's caller. */
    private UUID requestTenant;

    @BeforeEach
    void setUp() {
        requestTenant = TenantConstants.DEFAULT_TENANT_ID;
        restTemplate.getRestTemplate().getInterceptors().clear();
        restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set(TenantConstants.X_TENANT_ID_HEADER, requestTenant.toString());
            return execution.execute(request, body);
        });
    }

    @Test
    void beneficiary_createReadUpdate_andIsInvisibleToAnotherTenant() {
        ResponseEntity<BeneficiaryDto> created = restTemplate.postForEntity("/beneficiaries",
                new BeneficiaryCreateRequest("Harbour Trust", "Harbour Trust NGO", "hello@harbour.example",
                        null, "Kochi", null, "Coastal community support"),
                BeneficiaryDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = created.getBody().id();

        assertThat(restTemplate.getForEntity("/beneficiaries/{id}", BeneficiaryDto.class, id).getBody().name())
                .isEqualTo("Harbour Trust");
        assertThat(restTemplate.getForEntity("/beneficiaries?search=Kochi", String.class).getBody())
                .contains("Harbour Trust");

        ResponseEntity<BeneficiaryDto> updated = restTemplate.exchange("/beneficiaries/{id}", HttpMethod.PUT,
                new HttpEntity<>(new BeneficiaryUpdateRequest(null, null, null, null, "Kollam", null, null, null)),
                BeneficiaryDto.class, id);
        assertThat(updated.getBody().city()).isEqualTo("Kollam");
        assertThat(updated.getBody().name()).isEqualTo("Harbour Trust");

        requestTenant = UUID.randomUUID();
        assertThat(restTemplate.getForEntity("/beneficiaries/{id}", String.class, id).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.getForEntity("/beneficiaries/{id}/events", String.class, id).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void poc_assignListRejectDuplicate_andRemove() {
        PocAssignRequest request = new PocAssignRequest(SEEDED_POC_USER, AssignmentRole.PRIMARY);

        ResponseEntity<PocAssignmentDto> assigned = restTemplate.postForEntity(
                "/events/{eventId}/pocs", request, PocAssignmentDto.class, SEEDED_DRAFT_EVENT);
        assertThat(assigned.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(assigned.getBody().username()).isEqualTo("anita_desai");

        assertThat(restTemplate.postForEntity("/events/{eventId}/pocs", request, String.class, SEEDED_DRAFT_EVENT)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        List<PocAssignmentDto> pocs = restTemplate.exchange("/events/{eventId}/pocs", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PocAssignmentDto>>() {}, SEEDED_DRAFT_EVENT).getBody();
        assertThat(pocs).extracting(PocAssignmentDto::userId).contains(SEEDED_POC_USER);

        assertThat(restTemplate.exchange("/events/{eventId}/pocs/{userId}", HttpMethod.DELETE, null,
                Void.class, SEEDED_DRAFT_EVENT, SEEDED_POC_USER).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(restTemplate.exchange("/events/{eventId}/pocs/{userId}", HttpMethod.DELETE, null,
                String.class, SEEDED_DRAFT_EVENT, SEEDED_POC_USER).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
