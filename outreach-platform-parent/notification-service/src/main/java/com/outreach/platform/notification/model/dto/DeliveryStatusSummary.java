package com.outreach.platform.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Summary of delivery statuses for all emails associated with an event.
 */
@Schema(description = "Delivery status summary for all emails associated with an event")
public record DeliveryStatusSummary(
        @Schema(description = "Event ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String eventId,
        @Schema(description = "Total emails", example = "100")
        long total,
        @Schema(description = "Pending emails", example = "5")
        long pending,
        @Schema(description = "Queued emails", example = "10")
        long queued,
        @Schema(description = "Sent emails", example = "50")
        long sent,
        @Schema(description = "Delivered emails", example = "30")
        long delivered,
        @Schema(description = "Bounced emails", example = "3")
        long bounced,
        @Schema(description = "Failed emails", example = "1")
        long failed,
        @Schema(description = "Permanently failed emails", example = "1")
        long permanentlyFailed
) {
}
