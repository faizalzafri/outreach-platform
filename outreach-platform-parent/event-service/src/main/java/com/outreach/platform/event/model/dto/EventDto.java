package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only representation of an event.
 */
public record EventDto(
        UUID id,
        String eventCode,
        String eventName,
        String description,
        EventStatus status,
        LocalDate eventDate,
        LocalDate eventEndDate,
        String city,
        String venue,
        String category,
        Integer maxVolunteers,
        Integer registeredCount,
        Integer attendedCount,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
}
