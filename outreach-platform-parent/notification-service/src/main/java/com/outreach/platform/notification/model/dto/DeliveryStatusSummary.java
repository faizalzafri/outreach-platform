package com.outreach.platform.notification.model.dto;

/**
 * Summary of delivery statuses for all emails associated with an event.
 */
public record DeliveryStatusSummary(
        String eventId,
        long total,
        long pending,
        long queued,
        long sent,
        long delivered,
        long bounced,
        long failed,
        long permanentlyFailed
) {
}
