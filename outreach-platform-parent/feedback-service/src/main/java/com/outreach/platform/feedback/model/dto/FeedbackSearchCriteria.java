package com.outreach.platform.feedback.model.dto;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Search criteria for querying feedback records with multiple filters.
 */
@Schema(description = "Search criteria for filtering feedback records")
public record FeedbackSearchCriteria(
        @Schema(description = "Filter by event ID")
        UUID eventId,
        @Schema(description = "Filter by volunteer ID")
        UUID volunteerId,
        @Schema(description = "Filter by category", example = "event-quality")
        String category,
        @Schema(description = "Filter by sentiment", example = "POSITIVE")
        FeedbackSentiment sentiment,
        @Schema(description = "Filter by status", example = "SUBMITTED")
        FeedbackStatus status,
        @Schema(description = "Minimum score filter", example = "3")
        Integer minScore,
        @Schema(description = "Maximum score filter", example = "5")
        Integer maxScore,
        @Schema(description = "Start of date range")
        Instant dateFrom,
        @Schema(description = "End of date range")
        Instant dateTo
) {
}
