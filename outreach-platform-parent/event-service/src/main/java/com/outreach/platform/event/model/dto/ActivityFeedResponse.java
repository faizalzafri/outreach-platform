package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.ActivityEvent;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for the activity feed endpoint.
 * Contains a page of activity events and a cursor for fetching the next page.
 *
 * @param items      the list of activity events in this page
 * @param nextCursor the timestamp of the last item (used as cursor for next page), or null if no more items
 */
public record ActivityFeedResponse(
        List<ActivityEvent> items,
        Instant nextCursor
) {
}
