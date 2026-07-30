package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Net Promoter Score breakdown by event, calculated from feedback scores. */
@Schema(description = "Net Promoter Score breakdown for an event")
public record NpsResultDto(
        @Schema(description = "Event ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String eventId,
        @Schema(description = "Event name", example = "Community Health Drive")
        String eventName,
        @Schema(description = "Number of promoters (score 4-5)", example = "30")
        long promoters,
        @Schema(description = "Number of passives (score 3)", example = "8")
        long passives,
        @Schema(description = "Number of detractors (score 1-2)", example = "4")
        long detractors,
        @Schema(description = "Calculated NPS score", example = "61.9")
        BigDecimal npsScore,
        @Schema(description = "Total number of responses", example = "42")
        long totalResponses
) {
}
