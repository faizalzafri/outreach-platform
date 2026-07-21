package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Query parameters DTO for feedback search endpoint.
 */
@Schema(description = "Query parameters for the feedback search endpoint")
public record FeedbackSearchRequest(
        @Schema(description = "Filter by event ID")
        UUID eventId,
        @Schema(description = "Filter by employee ID")
        UUID employeeId,
        @Schema(description = "Filter by category", example = "event-quality")
        String category,
        @Schema(description = "Filter by sentiment", example = "POSITIVE")
        FeedbackSentiment sentiment,
        @Schema(description = "Filter by status", example = "SUBMITTED")
        FeedbackStatus status,
        @Schema(description = "Minimum score", example = "3")
        Integer minScore,
        @Schema(description = "Maximum score", example = "5")
        Integer maxScore,
        @Schema(description = "Start of date range")
        Instant dateFrom,
        @Schema(description = "End of date range")
        Instant dateTo
) {
}
