package com.outreach.platform.notification.model.dto;

/**
 * Response returned after a manual retry operation.
 */
public record RetryResponse(
        String eventId,
        int retriedCount,
        String message
) {
}
