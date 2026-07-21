package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request to enroll one or more volunteers in an event.
 */
public record VolunteerEnrollRequest(
        @NotEmpty List<String> employeeIds
) {
}
