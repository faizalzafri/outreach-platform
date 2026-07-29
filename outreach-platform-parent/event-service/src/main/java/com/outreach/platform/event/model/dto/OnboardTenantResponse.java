package com.outreach.platform.event.model.dto;

import java.util.UUID;

/**
 * Response DTO returned after a successful tenant onboarding operation.
 * Contains the created tenant's details and the admin membership information.
 */
public record OnboardTenantResponse(
        UUID tenantId,
        String tenantName,
        String tenantSlug,
        UUID adminUserId
) {
}
