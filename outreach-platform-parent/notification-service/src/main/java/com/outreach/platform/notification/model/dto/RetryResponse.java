package com.outreach.platform.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a manual retry operation.
 */
@Schema(description = "Response after a manual email retry operation")
public record RetryResponse(
        @Schema(description = "Event ID that was retried", example = "550e8400-e29b-41d4-a716-446655440000")
        String eventId,
        @Schema(description = "Number of emails retried", example = "3")
        int retriedCount,
        @Schema(description = "Status message", example = "3 emails queued for retry")
        String message
) {
}
