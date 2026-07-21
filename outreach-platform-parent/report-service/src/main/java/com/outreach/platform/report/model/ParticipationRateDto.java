package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Volunteer participation rate statistics.
 */
@Schema(description = "Volunteer participation rate statistics")
public record ParticipationRateDto(
        @Schema(description = "Total registered volunteers", example = "100")
        long totalRegistered,
        @Schema(description = "Total who actually attended", example = "82")
        long totalAttended,
        @Schema(description = "Participation rate as a percentage", example = "82.0")
        BigDecimal participationRate,
        @Schema(description = "Feedback submission rate as a percentage", example = "75.5")
        BigDecimal feedbackSubmissionRate
) {
}
