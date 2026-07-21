package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A geographic heatmap entry representing participation density by city.
 */
@Schema(description = "Geographic heatmap entry representing participation density by city")
public record HeatmapEntry(
        @Schema(description = "City name", example = "Bangalore")
        String city,
        @Schema(description = "Number of participants", example = "250")
        long participantCount,
        @Schema(description = "Number of events", example = "12")
        long eventCount,
        @Schema(description = "Intensity value (0.0 to 1.0)", example = "0.85")
        double intensity
) {
}
