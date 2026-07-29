package com.outreach.platform.event.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for team data.
 */
public record TeamResponse(
        UUID id,
        String name,
        String description,
        Instant createdDate
) {
}
