package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Sentiment analysis breakdown of feedback submissions.
 */
@Schema(description = "Sentiment analysis breakdown of feedback submissions")
public record SentimentBreakdownDto(
        @Schema(description = "Number of positive feedback", example = "80")
        long positive,
        @Schema(description = "Number of neutral feedback", example = "15")
        long neutral,
        @Schema(description = "Number of negative feedback", example = "5")
        long negative,
        @Schema(description = "Total feedback count", example = "100")
        long total,
        @Schema(description = "Positive feedback percentage", example = "80.0")
        BigDecimal positivePercentage,
        @Schema(description = "Neutral feedback percentage", example = "15.0")
        BigDecimal neutralPercentage,
        @Schema(description = "Negative feedback percentage", example = "5.0")
        BigDecimal negativePercentage
) {
}
