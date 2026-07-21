package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Leaderboard entry for top volunteers by participation count.
 */
@Schema(description = "Leaderboard entry showing a top volunteer by participation count")
public record VolunteerLeaderboardDto(
        @Schema(description = "Employee ID", example = "EMP-1234")
        String employeeId,
        @Schema(description = "Volunteer full name", example = "Rajesh Kumar")
        String fullName,
        @Schema(description = "Department", example = "Engineering")
        String department,
        @Schema(description = "Total events participated", example = "15")
        Integer totalEventsParticipated,
        @Schema(description = "Average feedback score", example = "4.7")
        BigDecimal avgFeedbackScore
) {
}
