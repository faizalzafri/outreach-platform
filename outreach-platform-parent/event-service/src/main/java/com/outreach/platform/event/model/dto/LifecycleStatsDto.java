package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregated counts of events per lifecycle status.
 */
@Schema(description = "Aggregated event counts grouped by lifecycle status")
public record LifecycleStatsDto(
        @Schema(description = "Number of events in DRAFT status", example = "5")
        long draft,
        @Schema(description = "Number of events in PUBLISHED status", example = "10")
        long published,
        @Schema(description = "Number of events in ACTIVE status", example = "8")
        long active,
        @Schema(description = "Number of events in COMPLETED status", example = "25")
        long completed,
        @Schema(description = "Number of events in ARCHIVED status", example = "12")
        long archived,
        @Schema(description = "Number of events in CANCELLED status", example = "3")
        long cancelled
) {
}
