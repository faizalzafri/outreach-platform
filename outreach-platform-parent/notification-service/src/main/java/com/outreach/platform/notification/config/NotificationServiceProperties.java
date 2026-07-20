package com.outreach.platform.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Externalized configuration properties for the Notification Service.
 * Bound from the {@code notification-service} prefix in application.yml.
 */
@ConfigurationProperties(prefix = "notification-service")
public record NotificationServiceProperties(
        int maxRetryAttempts,
        List<Integer> retryBackoffMinutes,
        String fromAddress,
        String fromName,
        int batchSize
) {

    /**
     * Provides sensible defaults when properties are not explicitly configured.
     */
    public NotificationServiceProperties {
        if (maxRetryAttempts <= 0) {
            maxRetryAttempts = 5;
        }
        if (retryBackoffMinutes == null || retryBackoffMinutes.isEmpty()) {
            retryBackoffMinutes = List.of(1, 5, 30, 120, 720);
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            fromAddress = "noreply@outreach.com";
        }
        if (fromName == null || fromName.isBlank()) {
            fromName = "Outreach Platform";
        }
        if (batchSize <= 0) {
            batchSize = 50;
        }
    }
}
