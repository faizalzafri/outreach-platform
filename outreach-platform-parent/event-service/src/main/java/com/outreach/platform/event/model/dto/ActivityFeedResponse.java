package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.ActivityEvent;

import java.time.Instant;
import java.util.List;

/** Response DTO for the activity feed containing events and a cursor for the next page. */
public record ActivityFeedResponse(
        List<ActivityEvent> items,
        Instant nextCursor
) {
}
