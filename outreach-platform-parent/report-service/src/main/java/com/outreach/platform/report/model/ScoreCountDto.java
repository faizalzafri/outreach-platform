package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** How many feedback submissions gave one score. */
@Schema(description = "How many feedback submissions gave one score")
public record ScoreCountDto(
        @Schema(description = "Feedback score, 1 to 5", example = "4")
        int score,
        @Schema(description = "Number of submissions with that score", example = "27")
        long count
) {
}
