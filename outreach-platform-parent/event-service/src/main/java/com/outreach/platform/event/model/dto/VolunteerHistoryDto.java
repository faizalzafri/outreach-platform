package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a volunteer's participation in a single event.
 */
@Schema(description = "A volunteer's participation record for a single event")
public record VolunteerHistoryDto(
        @Schema(description = "Event ID")
        UUID eventId,
        @Schema(description = "Event name", example = "Community Health Drive")
        String eventName,
        @Schema(description = "Event code", example = "EVT-2024-001")
        String eventCode,
        @Schema(description = "Event date", example = "2024-06-15")
        LocalDate eventDate,
        @Schema(description = "City where the event took place", example = "Bangalore")
        String city,
        @Schema(description = "Volunteer's attendance status", example = "ATTENDED")
        AttendanceStatus attendanceStatus,
        @Schema(description = "When the volunteer registered")
        Instant registeredAt
) {
}
