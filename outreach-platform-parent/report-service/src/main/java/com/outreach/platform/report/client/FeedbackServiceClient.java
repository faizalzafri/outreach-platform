package com.outreach.platform.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for inter-service calls to Feedback Service.
 * Used by Report Service to retrieve feedback data for aggregation and analytics.
 */
@FeignClient(
        name = "feedback-service",
        fallbackFactory = FeedbackServiceClientFallbackFactory.class
)
public interface FeedbackServiceClient {

    /**
     * List all feedback for an event (paginated).
     */
    @GetMapping("/feedback/event/{eventId}")
    PagedFeedbackResponse listByEvent(
            @PathVariable("eventId") UUID eventId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );

    /**
     * Get feedback completion status for an event.
     */
    @GetMapping("/feedback/event/{eventId}/status")
    FeedbackStatusResponse getEventFeedbackStatus(@PathVariable("eventId") UUID eventId);

    /**
     * Search feedback with filters.
     */
    @GetMapping("/feedback/search")
    PagedFeedbackResponse searchFeedback(
            @RequestParam(value = "eventId", required = false) UUID eventId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "minScore", required = false) Integer minScore,
            @RequestParam(value = "maxScore", required = false) Integer maxScore,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );

    // --- Response DTOs ---

    record FeedbackResponse(
            UUID id,
            UUID eventId,
            UUID volunteerId,
            int score,
            String answer1,
            String answer2,
            String answer3,
            String category,
            String tags,
            String sentiment,
            String status,
            boolean anonymous,
            String submittedAt,
            String createdAt
    ) {}

    record FeedbackStatusResponse(
            UUID eventId,
            long totalFeedback,
            long submitted,
            long reviewed,
            long flagged,
            long archived,
            double averageScore,
            double completionRate
    ) {}

    record PagedFeedbackResponse(
            List<FeedbackResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ) {}
}
