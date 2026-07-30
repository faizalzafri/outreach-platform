package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.ActionType;

import java.time.Instant;
import java.util.UUID;

/** Filter criteria for querying the activity feed. All fields are optional. */
public record ActivityFeedFilter(
        UUID teamId,
        ActionType actionType,
        String resourceType,
        Instant dateFrom,
        Instant dateTo
) {
}
