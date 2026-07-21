package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a single beneficiary.
 */
@Schema(description = "Aggregated feedback scores for a single beneficiary")
public record BeneficiaryScoreDto(
        @Schema(description = "Beneficiary ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String beneficiaryId,
        @Schema(description = "Beneficiary name", example = "Green Earth Foundation")
        String beneficiaryName,
        @Schema(description = "Average feedback score", example = "4.1")
        BigDecimal averageScore,
        @Schema(description = "Total feedback count", example = "85")
        long feedbackCount,
        @Schema(description = "Number of events associated", example = "5")
        long eventCount
) {
}
