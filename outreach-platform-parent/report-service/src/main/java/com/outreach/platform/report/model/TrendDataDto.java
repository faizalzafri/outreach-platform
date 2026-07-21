package com.outreach.platform.report.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Time-series trend data for dashboard visualization.
 */
public record TrendDataDto(
        String granularity,
        List<TrendPoint> points
) {

    public record TrendPoint(
            String period,
            long eventCount,
            long feedbackCount,
            BigDecimal averageScore
    ) {
    }
}
