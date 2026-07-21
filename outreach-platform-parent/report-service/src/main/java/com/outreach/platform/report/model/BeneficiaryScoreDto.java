package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a single beneficiary.
 */
public record BeneficiaryScoreDto(
        String beneficiaryId,
        String beneficiaryName,
        BigDecimal averageScore,
        long feedbackCount,
        long eventCount
) {
}
