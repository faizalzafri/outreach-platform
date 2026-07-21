package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.EmailStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only representation of a volunteer enrollment in an event.
 */
@Schema(description = "Read-only representation of a volunteer enrollment in an event")
public record EnrollmentDto(
        @Schema(description = "Enrollment unique identifier")
        UUID id,
        @Schema(description = "Associated event ID")
        UUID eventId,
        @Schema(description = "Volunteer ID")
        UUID volunteerId,
        @Schema(description = "Employee ID of the volunteer", example = "EMP-1234")
        String employeeId,
        @Schema(description = "Volunteer full name", example = "Rajesh Kumar")
        String volunteerName,
        @Schema(description = "Attendance status", example = "ATTENDED")
        AttendanceStatus attendanceStatus,
        @Schema(description = "Email notification status", example = "SENT")
        EmailStatus emailStatus,
        @Schema(description = "Registration timestamp")
        Instant registeredAt,
        @Schema(description = "Timestamp when attendance was marked")
        Instant attendanceMarkedAt
) {
}
