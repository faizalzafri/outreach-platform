package com.outreach.platform.event.model.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Read-only representation of an audit log entry from MongoDB.
 */
public record AuditLogEntry(
        String id,
        String userId,
        String action,
        String resourceType,
        String resourceId,
        Instant timestamp,
        Map<String, Object> details
) {
}
