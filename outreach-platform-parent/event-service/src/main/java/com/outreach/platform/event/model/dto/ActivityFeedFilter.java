package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.ActionType;

import java.time.Instant;
import java.util.UUID;

/**
 * Filter criteria for querying the activity feed.
 * All fields are optional — when null, the corresponding criterion is not applied.
 *
 * @param teamId       optional team ID to filter activities by team scope
 * @param actionType   optional action type to filter by (CREATED, MODIFIED, SHARED, etc.)
 * @param resourceType optional resource type to filter by (e.g., "SEQUENCE", "TEMPLATE")
 * @param dateFrom     optional start of date range (inclusive)
 * @param dateTo       optional end of date range (inclusive)
 */
public record ActivityFeedFilter(
        UUID teamId,
        ActionType actionType,
        String resourceType,
        Instant dateFrom,
        Instant dateTo
) {
}
