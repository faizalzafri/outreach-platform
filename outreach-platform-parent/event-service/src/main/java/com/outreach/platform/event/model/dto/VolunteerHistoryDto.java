package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AttendanceStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a volunteer's participation in a single event.
 */
public record VolunteerHistoryDto(
        UUID eventId,
        String eventName,
        String eventCode,
        LocalDate eventDate,
        String city,
        AttendanceStatus attendanceStatus,
        Instant registeredAt
) {
}
