package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a single event.
 */
@Schema(description = "Aggregated feedback scores for a single event")
public record EventScoreDto(
        @Schema(description = "Event ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String eventId,
        @Schema(description = "Event name", example = "Community Health Drive")
        String eventName,
        @Schema(description = "City", example = "Bangalore")
        String city,
        @Schema(description = "Average feedback score", example = "4.3")
        BigDecimal averageScore,
        @Schema(description = "Total feedback count", example = "42")
        long feedbackCount,
        @Schema(description = "Minimum score received", example = "2")
        int minScore,
        @Schema(description = "Maximum score received", example = "5")
        int maxScore
) {
}
