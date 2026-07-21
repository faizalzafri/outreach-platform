package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only representation of an event.
 */
@Schema(description = "Read-only representation of an outreach event")
public record EventDto(
        @Schema(description = "Event unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Human-readable event code", example = "EVT-2024-001")
        String eventCode,
        @Schema(description = "Event display name", example = "Community Health Drive")
        String eventName,
        @Schema(description = "Detailed event description", example = "Annual health awareness camp for the community")
        String description,
        @Schema(description = "Current lifecycle status", example = "ACTIVE")
        EventStatus status,
        @Schema(description = "Event start date", example = "2024-06-15")
        LocalDate eventDate,
        @Schema(description = "Event end date", example = "2024-06-17")
        LocalDate eventEndDate,
        @Schema(description = "City where the event takes place", example = "Bangalore")
        String city,
        @Schema(description = "Venue name or address", example = "Convention Center, MG Road")
        String venue,
        @Schema(description = "Event category", example = "Health")
        String category,
        @Schema(description = "Maximum volunteer capacity", example = "50")
        Integer maxVolunteers,
        @Schema(description = "Number of volunteers registered", example = "35")
        Integer registeredCount,
        @Schema(description = "Number of volunteers who attended", example = "30")
        Integer attendedCount,
        @Schema(description = "Timestamp when the event was created")
        Instant createdAt,
        @Schema(description = "Timestamp of last update")
        Instant updatedAt,
        @Schema(description = "Username of the creator", example = "admin")
        String createdBy
) {
}
