package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a POC (Point of Contact).
 */
public record PocScoreDto(
        String pocId,
        String pocName,
        BigDecimal averageScore,
        long feedbackCount,
        long eventCount
) {
}
