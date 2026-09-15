package com.outreach.platform.feedback;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.feedback.model.dto.FeedbackDto;
import com.outreach.platform.feedback.model.dto.FeedbackSubmitRequest;
import com.outreach.platform.feedback.model.dto.FeedbackUpdateRequest;
import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Feedback Service REST API.
 * Uses Testcontainers PostgreSQL and verifies all core feedback endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("Feedback Service Integration Tests")
class FeedbackServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("feedback_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID eventId;
    private UUID volunteerId;

    @BeforeEach
    void setUp() {
        // TestRestTemplate's underlying RestTemplate is a shared bean across all test methods in
        // this class — @BeforeEach runs once per test, so guard against stacking a duplicate
        // interceptor on every one of the 9 tests. Without a tenant header, TenantEntityListener
        // throws IllegalStateException on persist (VolunteerFeedbackEntity extends
        // TenantAwareBaseEntity), so this suite never exercised a real request end-to-end before.
        if (restTemplate.getRestTemplate().getInterceptors().isEmpty()) {
            restTemplate.getRestTemplate().getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add(TenantConstants.X_TENANT_ID_HEADER, TenantConstants.DEFAULT_TENANT_ID.toString());
                return execution.execute(request, body);
            });
        }

        eventId = UUID.randomUUID();
        volunteerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /feedback submits feedback successfully and returns 201")
    void submitFeedback_returnsCreated() {
        FeedbackSubmitRequest request = new FeedbackSubmitRequest(
                eventId, volunteerId, 4, "Great event", "Well organized", "More sessions please",
                "Leadership", "excellent,community", false
        );

        ResponseEntity<FeedbackDto> response = restTemplate.postForEntity(
                "/feedback", request, FeedbackDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        FeedbackDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.eventId()).isEqualTo(eventId);
        assertThat(body.volunteerId()).isEqualTo(volunteerId);
        assertThat(body.score()).isEqualTo(4);
        assertThat(body.category()).isEqualTo("Leadership");
        assertThat(body.status()).isEqualTo(FeedbackStatus.SUBMITTED);
        assertThat(body.submittedAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /feedback with invalid score returns 400 with field errors")
    void submitFeedback_invalidScore_returnsBadRequest() {
        FeedbackSubmitRequest invalidLow = new FeedbackSubmitRequest(
                eventId, volunteerId, 0, null, null, null, null, null, false
        );

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/feedback", invalidLow, ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("POST /feedback with score > 5 returns 400")
    void submitFeedback_scoreAboveMax_returnsBadRequest() {
        FeedbackSubmitRequest invalidHigh = new FeedbackSubmitRequest(
                eventId, volunteerId, 6, null, null, null, null, null, false
        );

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/feedback", invalidHigh, ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("GET /feedback/{eventId}/{volunteerId} returns the submitted feedback")
    void getFeedback_afterSubmit_returnsFeedback() {
        FeedbackSubmitRequest request = new FeedbackSubmitRequest(
                eventId, volunteerId, 5, "Loved it", null, null, "Teamwork", null, false
        );
        restTemplate.postForEntity("/feedback", request, FeedbackDto.class);

        ResponseEntity<FeedbackDto> response = restTemplate.getForEntity(
                "/feedback/{eventId}/{volunteerId}", FeedbackDto.class, eventId, volunteerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.eventId()).isEqualTo(eventId);
        assertThat(body.volunteerId()).isEqualTo(volunteerId);
        assertThat(body.score()).isEqualTo(5);
        assertThat(body.answer1()).isEqualTo("Loved it");
        assertThat(body.category()).isEqualTo("Teamwork");
    }

    @Test
    @DisplayName("GET /feedback/event/{eventId} returns paginated list of feedback")
    void listByEvent_returnsPaginatedResults() {
        UUID sharedEventId = UUID.randomUUID();

        for (int i = 1; i <= 3; i++) {
            FeedbackSubmitRequest request = new FeedbackSubmitRequest(
                    sharedEventId, UUID.randomUUID(), i + 2, "Answer " + i, null, null,
                    "General", null, false
            );
            restTemplate.postForEntity("/feedback", request, FeedbackDto.class);
        }

        ResponseEntity<RestPageResponse<FeedbackDto>> response = restTemplate.exchange(
                "/feedback/event/{eventId}?page=0&size=2",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {},
                sharedEventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        RestPageResponse<FeedbackDto> page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("GET /feedback/search with category filter returns matching results")
    void search_byCategoryFilter_returnsFilteredResults() {
        UUID searchEventId = UUID.randomUUID();

        restTemplate.postForEntity("/feedback", new FeedbackSubmitRequest(
                searchEventId, UUID.randomUUID(), 5, null, null, null, "Leadership", null, false
        ), FeedbackDto.class);

        restTemplate.postForEntity("/feedback", new FeedbackSubmitRequest(
                searchEventId, UUID.randomUUID(), 3, null, null, null, "Teamwork", null, false
        ), FeedbackDto.class);

        restTemplate.postForEntity("/feedback", new FeedbackSubmitRequest(
                searchEventId, UUID.randomUUID(), 4, null, null, null, "Leadership", null, false
        ), FeedbackDto.class);

        ResponseEntity<RestPageResponse<FeedbackDto>> response = restTemplate.exchange(
                "/feedback/search?category=Leadership&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {},
                searchEventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        RestPageResponse<FeedbackDto> page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.getContent()).allSatisfy(dto ->
                assertThat(dto.category()).isEqualTo("Leadership")
        );
        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("GET /feedback/search with minScore/maxScore filter returns results in range")
    void search_byScoreRange_returnsFilteredResults() {
        UUID scoreEventId = UUID.randomUUID();

        restTemplate.postForEntity("/feedback", new FeedbackSubmitRequest(
                scoreEventId, UUID.randomUUID(), 1, null, null, null, "General", null, false
        ), FeedbackDto.class);

        restTemplate.postForEntity("/feedback", new FeedbackSubmitRequest(
                scoreEventId, UUID.randomUUID(), 3, null, null, null, "General", null, false
        ), FeedbackDto.class);

        restTemplate.postForEntity("/feedback", new FeedbackSubmitRequest(
                scoreEventId, UUID.randomUUID(), 5, null, null, null, "General", null, false
        ), FeedbackDto.class);

        ResponseEntity<RestPageResponse<FeedbackDto>> response = restTemplate.exchange(
                "/feedback/search?minScore=3&maxScore=5&eventId={eventId}&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {},
                scoreEventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        RestPageResponse<FeedbackDto> page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allSatisfy(dto -> {
            assertThat(dto.score()).isGreaterThanOrEqualTo(3);
            assertThat(dto.score()).isLessThanOrEqualTo(5);
        });
    }

    @Test
    @DisplayName("PUT /feedback/{eventId}/{volunteerId} updates feedback successfully")
    void updateFeedback_returnsUpdatedRecord() {
        FeedbackSubmitRequest submitRequest = new FeedbackSubmitRequest(
                eventId, volunteerId, 3, "Initial answer", null, null, "General", null, false
        );
        restTemplate.postForEntity("/feedback", submitRequest, FeedbackDto.class);

        FeedbackUpdateRequest updateRequest = new FeedbackUpdateRequest(
                5, "Updated answer", null, null, "Leadership", "high-impact",
                FeedbackSentiment.POSITIVE, FeedbackStatus.REVIEWED
        );

        ResponseEntity<FeedbackDto> response = restTemplate.exchange(
                "/feedback/{eventId}/{volunteerId}",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                FeedbackDto.class,
                eventId, volunteerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.score()).isEqualTo(5);
        assertThat(body.answer1()).isEqualTo("Updated answer");
        assertThat(body.category()).isEqualTo("Leadership");
        assertThat(body.tags()).isEqualTo("high-impact");
        assertThat(body.sentiment()).isEqualTo(FeedbackSentiment.POSITIVE);
        assertThat(body.status()).isEqualTo(FeedbackStatus.REVIEWED);
    }

    @Test
    @DisplayName("Duplicate submission returns 409 Conflict")
    void submitFeedback_duplicate_returnsConflict() {
        FeedbackSubmitRequest request = new FeedbackSubmitRequest(
                eventId, volunteerId, 4, "First submission", null, null, "General", null, false
        );
        restTemplate.postForEntity("/feedback", request, FeedbackDto.class);

        FeedbackSubmitRequest duplicate = new FeedbackSubmitRequest(
                eventId, volunteerId, 5, "Duplicate attempt", null, null, "General", null, false
        );
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/feedback", duplicate, ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Feedback Already Exists");
    }
}
