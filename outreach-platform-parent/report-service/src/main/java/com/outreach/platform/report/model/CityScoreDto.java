package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a city/location.
 */
@Schema(description = "Aggregated feedback scores for a city/location")
public record CityScoreDto(
        @Schema(description = "City name", example = "Bangalore")
        String city,
        @Schema(description = "Average feedback score", example = "4.4")
        BigDecimal averageScore,
        @Schema(description = "Total feedback count", example = "320")
        long feedbackCount,
        @Schema(description = "Number of events in this city", example = "15")
        long eventCount,
        @Schema(description = "Number of volunteers in this city", example = "180")
        long volunteerCount
) {
}
