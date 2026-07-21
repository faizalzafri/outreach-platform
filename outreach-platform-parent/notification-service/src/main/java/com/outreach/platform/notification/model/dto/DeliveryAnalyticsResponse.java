package com.outreach.platform.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Analytics response showing delivery rate percentages across all emails.
 */
@Schema(description = "Email delivery analytics showing rate percentages")
public record DeliveryAnalyticsResponse(
        @Schema(description = "Total emails processed", example = "1000")
        long totalEmails,
        @Schema(description = "Percentage sent successfully", example = "95.5")
        double sentPercentage,
        @Schema(description = "Percentage confirmed delivered", example = "90.2")
        double deliveredPercentage,
        @Schema(description = "Percentage bounced", example = "3.1")
        double bouncedPercentage,
        @Schema(description = "Percentage failed", example = "1.2")
        double failedPercentage,
        @Schema(description = "Permanently failed email count", example = "5")
        long permanentlyFailedCount
) {
}
