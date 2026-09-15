package com.outreach.platform.event.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a team membership record, enriched with the member's username/email so the
 * frontend doesn't need a separate lookup per member.
 */
public record TeamMemberResponse(
        UUID userId,
        String username,
        String email,
        Instant joinedAt
) {
}
