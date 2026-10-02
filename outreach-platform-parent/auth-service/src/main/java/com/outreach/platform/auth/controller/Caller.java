package com.outreach.platform.auth.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Who is calling an {@code /api/auth/**} endpoint, read from their access token.
 *
 * @param userId   the account id ({@code uid} claim)
 * @param tenantId the active tenant, or null for platform admins
 */
record Caller(UUID userId, String username, UUID tenantId, boolean platformAdmin) {

    static Caller of(Jwt jwt) {
        String uid = jwt.getClaimAsString("uid");
        String tenant = jwt.getClaimAsString("tenant_id");
        return new Caller(
                uid != null ? UUID.fromString(uid) : null,
                jwt.getSubject(),
                tenant != null ? UUID.fromString(tenant) : null,
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("platform_admin")));
    }

    /**
     * The tenant an admin operation applies to: a tenant admin's own tenant (a different one is
     * refused), or the tenant a platform admin names explicitly.
     */
    UUID tenantFor(UUID requested) {
        if (platformAdmin) {
            if (requested == null) {
                throw new IllegalArgumentException("tenantId is required for platform administrators");
            }
            return requested;
        }
        if (tenantId == null || (requested != null && !requested.equals(tenantId))) {
            throw new AccessDeniedException("You can only manage users in your own organization");
        }
        return tenantId;
    }
}
