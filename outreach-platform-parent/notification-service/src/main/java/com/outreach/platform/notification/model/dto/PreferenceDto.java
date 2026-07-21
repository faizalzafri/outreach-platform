package com.outreach.platform.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Read model representing notification preferences for an employee.
 */
@Schema(description = "Notification preferences for an employee")
public record PreferenceDto(
        @Schema(description = "Employee ID", example = "EMP-1234")
        String employeeId,
        @Schema(description = "Whether email notifications are enabled", example = "true")
        boolean emailEnabled,
        @Schema(description = "List of allowed notification types", example = "[\"EMAIL\", \"SMS\"]")
        List<String> allowedTypes,
        @Schema(description = "Timestamp of last preference update")
        Instant updatedAt
) {
}
