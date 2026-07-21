package com.outreach.platform.report.model;

/**
 * A geographic heatmap entry representing participation density by city.
 */
public record HeatmapEntry(
        String city,
        long participantCount,
        long eventCount,
        double intensity
) {
}
