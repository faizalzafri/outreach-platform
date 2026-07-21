package com.outreach.platform.event.model.dto;

import java.math.BigDecimal;

/**
 * Leaderboard entry for top volunteers by participation count.
 */
public record VolunteerLeaderboardDto(
        String employeeId,
        String fullName,
        String department,
        Integer totalEventsParticipated,
        BigDecimal avgFeedbackScore
) {
}
