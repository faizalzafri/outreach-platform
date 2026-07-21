package com.outreach.platform.report.model;

import java.math.BigDecimal;

/**
 * Sentiment analysis breakdown of feedback submissions.
 */
public record SentimentBreakdownDto(
        long positive,
        long neutral,
        long negative,
        long total,
        BigDecimal positivePercentage,
        BigDecimal neutralPercentage,
        BigDecimal negativePercentage
) {
}
