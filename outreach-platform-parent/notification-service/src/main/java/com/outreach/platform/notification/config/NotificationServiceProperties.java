package com.outreach.platform.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Externalized configuration properties bound from the {@code notification-service} prefix.
 */
@ConfigurationProperties(prefix = "notification-service")
public record NotificationServiceProperties(
        int maxRetryAttempts,
        List<Integer> retryBackoffMinutes,
        String fromAddress,
        String fromName,
        int batchSize
) {


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
