package com.outreach.platform.event.model.dto;

import java.util.UUID;

/** Response DTO for a successful tenant onboarding operation. */
public record OnboardTenantResponse(
        UUID tenantId,
        String tenantName,
        String tenantSlug,
        UUID adminUserId
) {
}
