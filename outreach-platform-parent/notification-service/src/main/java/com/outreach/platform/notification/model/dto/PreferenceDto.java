package com.outreach.platform.notification.model.dto;

import java.time.Instant;
import java.util.List;

/**
 * Read model representing notification preferences for an employee.
 */
public record PreferenceDto(
        String employeeId,
        boolean emailEnabled,
        List<String> allowedTypes,
        Instant updatedAt
) {
}
