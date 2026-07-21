package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * A single data point in a custom time-series report.
 */
@Schema(description = "A single data point in a time-series report")
public record TimeSeriesDataPoint(
        @Schema(description = "Time period label", example = "2024-06")
        String period,
        @Schema(description = "Metric value", example = "4.5")
        BigDecimal value,
        @Schema(description = "Count of items", example = "42")
        long count
) {
}
