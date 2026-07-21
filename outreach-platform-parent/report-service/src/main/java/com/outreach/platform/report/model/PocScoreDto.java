package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a POC (Point of Contact).
 */
@Schema(description = "Aggregated feedback scores for a Point of Contact (POC)")
public record PocScoreDto(
        @Schema(description = "POC user ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String pocId,
        @Schema(description = "POC name", example = "Jane Smith")
        String pocName,
        @Schema(description = "Average feedback score", example = "4.6")
        BigDecimal averageScore,
        @Schema(description = "Total feedback count", example = "120")
        long feedbackCount,
        @Schema(description = "Number of events managed", example = "8")
        long eventCount
) {
}
