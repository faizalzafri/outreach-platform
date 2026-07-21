package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Key performance indicators for the platform.
 */
@Schema(description = "Key performance indicators for the outreach platform")
public record KpiDto(
        @Schema(description = "Average feedback score across all events", example = "4.2")
        BigDecimal averageFeedbackScore,
        @Schema(description = "Feedback completion rate as a percentage", example = "85.5")
        BigDecimal feedbackCompletionRate,
        @Schema(description = "Volunteer retention rate as a percentage", example = "72.0")
        BigDecimal volunteerRetentionRate,
        @Schema(description = "Average events per volunteer", example = "3.2")
        BigDecimal averageEventsPerVolunteer,
        @Schema(description = "Total feedback received this month", example = "150")
        long totalFeedbackThisMonth,
        @Schema(description = "Total events created this month", example = "8")
        long totalEventsThisMonth
) {
}
