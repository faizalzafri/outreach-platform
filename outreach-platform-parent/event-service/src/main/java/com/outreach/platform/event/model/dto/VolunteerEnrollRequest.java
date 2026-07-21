package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request to enroll one or more volunteers in an event.
 */
@Schema(description = "Request to enroll one or more volunteers in an event by employee IDs")
public record VolunteerEnrollRequest(
        @Schema(description = "List of employee IDs to enroll", example = "[\"EMP-1234\", \"EMP-5678\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty List<String> employeeIds
) {
}
