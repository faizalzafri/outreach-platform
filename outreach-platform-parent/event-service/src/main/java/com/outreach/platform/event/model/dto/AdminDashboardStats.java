package com.outreach.platform.event.model.dto;

/**
 * Admin dashboard aggregate statistics.
 */
public record AdminDashboardStats(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long totalEvents,
        long activeEvents,
        long totalVolunteers
) {
}
