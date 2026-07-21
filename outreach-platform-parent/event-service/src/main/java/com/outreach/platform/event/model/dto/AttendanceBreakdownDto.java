package com.outreach.platform.event.model.dto;

/**
 * Attendance breakdown for an event showing counts by status.
 */
public record AttendanceBreakdownDto(
        long totalRegistered,
        long attended,
        long notAttended,
        long unregistered
) {
}
