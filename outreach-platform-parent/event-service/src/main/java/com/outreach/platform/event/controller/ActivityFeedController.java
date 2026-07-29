package com.outreach.platform.event.controller;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.model.ActionType;
import com.outreach.platform.event.model.dto.ActivityFeedFilter;
import com.outreach.platform.event.model.dto.ActivityFeedResponse;
import com.outreach.platform.event.service.ActivityFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for the activity feed.
 * Returns a cursor-paginated feed of activity events visible to the current user.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/activity")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Activity Feed", description = "Query chronological activity events within the tenant")
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;

    @Inject
    public ActivityFeedController(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @Operation(
            summary = "Get activity feed",
            description = "Returns a cursor-paginated activity feed for the current user. "
                    + "Use the nextCursor value from the response as the cursor parameter to fetch the next page."
    )
    @GetMapping
    public ResponseEntity<ActivityFeedResponse> getActivityFeed(
            @Parameter(description = "Cursor for pagination — timestamp of the last item from previous page")
            @RequestParam(required = false) Instant cursor,

            @Parameter(description = "Maximum number of items to return (default 50)")
            @RequestParam(defaultValue = "50") int limit,

            @Parameter(description = "Filter by team ID")
            @RequestParam(required = false) UUID teamId,

            @Parameter(description = "Filter by action type")
            @RequestParam(required = false) ActionType actionType,

            @Parameter(description = "Filter by resource type (e.g. SEQUENCE, TEMPLATE)")
            @RequestParam(required = false) String resourceType,

            @Parameter(description = "Filter by date range start (inclusive)")
            @RequestParam(required = false) Instant from,

            @Parameter(description = "Filter by date range end (inclusive)")
            @RequestParam(required = false) Instant to) {

        UUID tenantId = TenantContext.getCurrentTenantId();
        UUID userId = getCurrentUserId();

        ActivityFeedFilter filter = new ActivityFeedFilter(teamId, actionType, resourceType, from, to);
        ActivityFeedResponse response = activityFeedService.getFeed(tenantId, userId, filter, cursor, limit);

        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the current user's UUID from the SecurityContext authentication principal.
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }
        return UUID.fromString(authentication.getName());
    }
}
