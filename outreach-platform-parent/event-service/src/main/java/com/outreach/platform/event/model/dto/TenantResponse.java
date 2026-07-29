package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.TenantStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for tenant data.
 */
public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        Instant createdDate
) {
}
