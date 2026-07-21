package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a single event.
 */
public record EventScoreDto(
        String eventId,
        String eventName,
        String city,
        BigDecimal averageScore,
        long feedbackCount,
        int minScore,
        int maxScore
) {
}
