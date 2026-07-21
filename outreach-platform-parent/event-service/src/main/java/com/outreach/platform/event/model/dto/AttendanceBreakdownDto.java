package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Attendance breakdown for an event showing counts by status.
 */
@Schema(description = "Attendance breakdown for an event showing counts by status")
public record AttendanceBreakdownDto(
        @Schema(description = "Total registered volunteers", example = "50")
        long totalRegistered,
        @Schema(description = "Volunteers who attended", example = "42")
        long attended,
        @Schema(description = "Volunteers who did not attend", example = "6")
        long notAttended,
        @Schema(description = "Volunteers who unregistered", example = "2")
        long unregistered
) {
}
