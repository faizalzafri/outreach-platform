package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Result of a volunteer import: the upserted volunteer, the event it was enrolled in, and
 * whether that enrollment already existed (idempotent re-import).
 */
@Schema(description = "Result of a volunteer import")
public record VolunteerImportResponse(
        UUID volunteerId,
        UUID eventId,
        @Schema(description = "True if the volunteer was already enrolled in this event before this call")
        boolean alreadyEnrolled
) {
}
