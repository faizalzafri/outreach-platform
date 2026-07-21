package com.outreach.platform.notification.model.dto;

/**
 * Analytics response showing delivery rate percentages across all emails.
 */
public record DeliveryAnalyticsResponse(
        long totalEmails,
        double sentPercentage,
        double deliveredPercentage,
        double bouncedPercentage,
        double failedPercentage,
        long permanentlyFailedCount
) {
}
