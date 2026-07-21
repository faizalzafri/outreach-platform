package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Admin dashboard aggregate statistics.
 */
@Schema(description = "Admin dashboard aggregate statistics")
public record AdminDashboardStats(
        @Schema(description = "Total registered users", example = "150")
        long totalUsers,
        @Schema(description = "Currently active users", example = "140")
        long activeUsers,
        @Schema(description = "Locked user accounts", example = "3")
        long lockedUsers,
        @Schema(description = "Total events created", example = "45")
        long totalEvents,
        @Schema(description = "Currently active events", example = "8")
        long activeEvents,
        @Schema(description = "Total registered volunteers", example = "500")
        long totalVolunteers
) {
}
