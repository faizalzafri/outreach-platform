package com.outreach.platform.feedback.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO representing feedback completion statistics for a given event.
 */
@Schema(description = "Feedback completion statistics for a given event")
public record FeedbackStatsDto(
        @Schema(description = "Event ID")
        UUID eventId,
        @Schema(description = "Total volunteers registered for the event", example = "50")
        long totalVolunteers,
        @Schema(description = "Number of feedback submissions received", example = "42")
        long submittedCount,
        @Schema(description = "Number of pending submissions", example = "8")
        long pendingCount,
        @Schema(description = "Average feedback score", example = "4.2")
        double averageScore
) {
}
