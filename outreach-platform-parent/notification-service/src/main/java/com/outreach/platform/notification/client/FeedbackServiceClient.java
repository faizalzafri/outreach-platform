package com.outreach.platform.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for inter-service calls to Feedback Service.
 * Used by Notification Service to check feedback completion status.
 */
@FeignClient(
        name = "feedback-service",
        fallbackFactory = FeedbackServiceClientFallbackFactory.class
)
public interface FeedbackServiceClient {

    /**
     * Get feedback completion status for an event.
     */
    @GetMapping("/feedback/event/{eventId}/status")
    FeedbackStatusResponse getEventFeedbackStatus(@PathVariable("eventId") UUID eventId);

    // --- Response DTO ---

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
}
