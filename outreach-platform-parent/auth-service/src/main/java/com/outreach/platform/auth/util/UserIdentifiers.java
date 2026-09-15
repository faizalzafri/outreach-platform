package com.outreach.platform.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The Spring Security JDBC user tables ({@code auth_users}/{@code auth_authorities}) have no UUID
 * primary key — just a username. {@code tenant_memberships.user_id} needs a stable UUID to key
 * off, so both token issuance ({@code AuthorizationServerConfig}) and the tenant-selection API
 * ({@code TenantSelectionController}) derive the same deterministic UUID from the username via
 * {@link UUID#nameUUIDFromBytes}, rather than each computing it separately and risking drift.
 */
public final class UserIdentifiers {

    private UserIdentifiers() {
    }

    public static UUID fromUsername(String username) {
        return UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8));
    }
}
