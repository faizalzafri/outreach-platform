package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * High-level dashboard summary statistics.
 */
public record DashboardSummaryDto(
        long totalEvents,
        long completedEvents,
        long totalVolunteers,
        long totalFeedbackSubmissions,
        BigDecimal overallAverageScore,
        long totalBeneficiaries,
        long activeCities
) {
}
