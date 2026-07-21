package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Aggregated feedback scores for a city/location.
 */
public record CityScoreDto(
        String city,
        BigDecimal averageScore,
        long feedbackCount,
        long eventCount,
        long volunteerCount
) {
}
