package com.outreach.platform.feedback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for the Feedback Service.
 *
 * @param maxScoreValue   maximum allowed feedback score (default 5)
 * @param minScoreValue   minimum allowed feedback score (default 1)
 * @param defaultPageSize default page size for paginated queries (default 20)
 * @param maxPageSize     maximum allowed page size for paginated queries (default 100)
 */
@ConfigurationProperties(prefix = "feedback-service")
public record FeedbackServiceProperties(
        int maxScoreValue,
        int minScoreValue,
        int defaultPageSize,
        int maxPageSize
) {
    public FeedbackServiceProperties {
        if (maxScoreValue == 0) maxScoreValue = 5;
        if (minScoreValue == 0) minScoreValue = 1;
        if (defaultPageSize == 0) defaultPageSize = 20;
        if (maxPageSize == 0) maxPageSize = 100;
    }
}
