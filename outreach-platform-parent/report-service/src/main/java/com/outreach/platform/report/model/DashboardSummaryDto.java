package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * High-level dashboard summary statistics.
 */
@Schema(description = "High-level dashboard summary statistics")
public record DashboardSummaryDto(
        @Schema(description = "Total events in the system", example = "45")
        long totalEvents,
        @Schema(description = "Completed events", example = "25")
        long completedEvents,
        @Schema(description = "Total registered volunteers", example = "500")
        long totalVolunteers,
        @Schema(description = "Total feedback submissions received", example = "1200")
        long totalFeedbackSubmissions,
        @Schema(description = "Overall average feedback score", example = "4.2")
        BigDecimal overallAverageScore,
        @Schema(description = "Total beneficiary organizations", example = "30")
        long totalBeneficiaries,
        @Schema(description = "Number of active cities", example = "12")
        long activeCities
) {
}
