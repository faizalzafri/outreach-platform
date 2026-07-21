package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.EmailStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only representation of a volunteer enrollment in an event.
 */
public record EnrollmentDto(
        UUID id,
        UUID eventId,
        UUID volunteerId,
        String employeeId,
        String volunteerName,
        AttendanceStatus attendanceStatus,
        EmailStatus emailStatus,
        Instant registeredAt,
        Instant attendanceMarkedAt
) {
}
