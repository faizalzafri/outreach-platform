package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Read-only representation of an audit log entry from MongoDB.
 */
@Schema(description = "Read-only representation of an audit log entry")
public record AuditLogEntry(
        @Schema(description = "Audit log entry ID", example = "6614a1b2c3d4e5f6a7b8c9d0")
        String id,
        @Schema(description = "User who performed the action", example = "admin")
        String userId,
        @Schema(description = "Action performed", example = "USER_CREATED")
        String action,
        @Schema(description = "Type of resource affected", example = "USER")
        String resourceType,
        @Schema(description = "ID of the affected resource", example = "550e8400-e29b-41d4-a716-446655440000")
        String resourceId,
        @Schema(description = "Timestamp of the action")
        Instant timestamp,
        @Schema(description = "Additional details about the action")
        Map<String, Object> details
) {
}
