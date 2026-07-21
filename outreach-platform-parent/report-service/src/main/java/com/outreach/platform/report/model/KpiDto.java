package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Key performance indicators for the platform.
 */
public record KpiDto(
        BigDecimal averageFeedbackScore,
        BigDecimal feedbackCompletionRate,
        BigDecimal volunteerRetentionRate,
        BigDecimal averageEventsPerVolunteer,
        long totalFeedbackThisMonth,
        long totalEventsThisMonth
) {
}
