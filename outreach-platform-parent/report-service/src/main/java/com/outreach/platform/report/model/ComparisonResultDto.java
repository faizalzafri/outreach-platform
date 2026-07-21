package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of comparing events or time periods.
 */
@Schema(description = "Comparison result for events or time periods")
public record ComparisonResultDto(
        @Schema(description = "Comparison items")
        List<ComparisonItem> items
) {

    @Schema(description = "Single item in a comparison")
    public record ComparisonItem(
            @Schema(description = "Comparison label", example = "Q1 2024")
            String label,
            @Schema(description = "Average feedback score", example = "4.2")
            BigDecimal averageScore,
            @Schema(description = "Feedback count", example = "150")
            long feedbackCount,
            @Schema(description = "Volunteer count", example = "80")
            long volunteerCount
    ) {
    }
}
