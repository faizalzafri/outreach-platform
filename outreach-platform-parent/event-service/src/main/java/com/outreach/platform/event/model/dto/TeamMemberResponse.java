package com.outreach.platform.event.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a team membership record.
 */
public record TeamMemberResponse(
        UUID userId,
        Instant joinedAt
) {
}
