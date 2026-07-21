package com.outreach.platform.event.model.dto;

import java.time.Instant;

/**
 * Search criteria for querying audit log entries.
 */
public record AuditLogSearchCriteria(
        String userId,
        String action,
        String resourceType,
        Instant dateFrom,
        Instant dateTo
) {
}
