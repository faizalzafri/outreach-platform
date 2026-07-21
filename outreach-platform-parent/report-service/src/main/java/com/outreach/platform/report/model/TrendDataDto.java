package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Time-series trend data for dashboard visualization.
 */
@Schema(description = "Time-series trend data for dashboard visualization")
public record TrendDataDto(
        @Schema(description = "Time granularity", example = "month")
        String granularity,
        @Schema(description = "Trend data points")
        List<TrendPoint> points
) {

    @Schema(description = "A single trend data point")
    public record TrendPoint(
            @Schema(description = "Time period label", example = "2024-06")
            String period,
            @Schema(description = "Number of events in this period", example = "8")
            long eventCount,
            @Schema(description = "Number of feedback submissions", example = "95")
            long feedbackCount,
            @Schema(description = "Average feedback score", example = "4.3")
            BigDecimal averageScore
    ) {
    }
}
