package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * A single data point in a custom time-series report.
 */
public record TimeSeriesDataPoint(
        String period,
        BigDecimal value,
        long count
) {
}
