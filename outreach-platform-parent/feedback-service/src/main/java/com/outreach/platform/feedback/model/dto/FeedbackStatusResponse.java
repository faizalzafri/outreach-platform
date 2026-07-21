package com.outreach.platform.feedback.model.dto;

/**
 * Response DTO for feedback completion statistics per event.
 */
public record FeedbackStatusResponse(
        java.util.UUID eventId,
        long totalFeedback,
        long submitted,
        long reviewed,
        long flagged,
        long archived,
        double averageScore,
        double completionRate
) {
}
