package com.outreach.platform.report.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of comparing events or time periods.
 */
public record ComparisonResultDto(
        List<ComparisonItem> items
) {

    public record ComparisonItem(
            String label,
            BigDecimal averageScore,
            long feedbackCount,
            long volunteerCount
    ) {
    }
}
