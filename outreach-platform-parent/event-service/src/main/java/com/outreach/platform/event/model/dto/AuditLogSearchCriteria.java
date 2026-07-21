package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Search criteria for querying audit log entries.
 */
@Schema(description = "Search criteria for filtering audit log entries")
public record AuditLogSearchCriteria(
        @Schema(description = "Filter by user ID", example = "admin")
        String userId,
        @Schema(description = "Filter by action type", example = "USER_CREATED")
        String action,
        @Schema(description = "Filter by resource type", example = "EVENT")
        String resourceType,
        @Schema(description = "Start of date range")
        Instant dateFrom,
        @Schema(description = "End of date range")
        Instant dateTo
) {
}
