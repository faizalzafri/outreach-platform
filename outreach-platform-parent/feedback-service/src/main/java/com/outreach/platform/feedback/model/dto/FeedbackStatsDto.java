package com.outreach.platform.feedback.model.dto;

import java.util.UUID;

/**
 * DTO representing feedback completion statistics for a given event.
 */
public record FeedbackStatsDto(
        UUID eventId,
        long totalVolunteers,
        long submittedCount,
        long pendingCount,
        double averageScore
) {
}
