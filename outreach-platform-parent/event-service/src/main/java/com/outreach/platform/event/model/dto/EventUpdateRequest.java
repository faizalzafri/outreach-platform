package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for updating an existing event.
 */
public record EventUpdateRequest(
        @Size(max = 255) String eventName,
        String description,
        LocalDate eventDate,
        LocalDate eventEndDate,
        @Size(max = 100) String city,
        @Size(max = 255) String venue,
        @Size(max = 50) String category,
        Integer maxVolunteers
) {
}
