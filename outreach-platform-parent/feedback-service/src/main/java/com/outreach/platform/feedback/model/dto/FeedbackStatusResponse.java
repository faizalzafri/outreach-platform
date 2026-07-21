package com.outreach.platform.feedback.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for feedback completion statistics per event.
 */
@Schema(description = "Feedback completion statistics and status breakdown per event")
public record FeedbackStatusResponse(
        @Schema(description = "Event ID")
        java.util.UUID eventId,
        @Schema(description = "Total feedback records", example = "50")
        long totalFeedback,
        @Schema(description = "Submitted feedback count", example = "42")
        long submitted,
        @Schema(description = "Reviewed feedback count", example = "30")
        long reviewed,
        @Schema(description = "Flagged feedback count", example = "2")
        long flagged,
        @Schema(description = "Archived feedback count", example = "5")
        long archived,
        @Schema(description = "Average feedback score", example = "4.1")
        double averageScore,
        @Schema(description = "Completion rate percentage", example = "84.0")
        double completionRate
) {
}
