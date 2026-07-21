package com.outreach.platform.event.model.dto;

/**
 * Aggregated counts of events per lifecycle status.
 */
public record LifecycleStatsDto(
        long draft,
        long published,
        long active,
        long completed,
        long archived,
        long cancelled
) {
}
