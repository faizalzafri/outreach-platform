package com.outreach.platform.auth.model.dto;

import com.outreach.platform.auth.model.TenantStatus;

import java.util.UUID;

/** One of the authenticated user's ACTIVE tenant memberships, for the tenant-selection page. */
public record TenantMembershipResponse(
        UUID tenantId,
        String tenantName,
        TenantStatus tenantStatus,
        String role
) {
}
