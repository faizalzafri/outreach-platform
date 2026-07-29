package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.TenantRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a tenant membership record.
 */
public record MemberResponse(
        UUID userId,
        TenantRole role,
        Instant joinedAt
) {
}
