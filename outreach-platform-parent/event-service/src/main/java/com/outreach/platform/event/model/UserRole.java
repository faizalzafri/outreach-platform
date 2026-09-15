package com.outreach.platform.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Roles assignable to platform users.
 *
 * <p>The {@code users.role} column (and this enum's {@code name()}, via
 * {@code @Enumerated(EnumType.STRING)} on {@link com.outreach.platform.event.entity.UserEntity})
 * stores the bare constant name — that's independent of JSON. Over the wire this uses the
 * "ROLE_"-prefixed form to match the platform-wide authority convention (JWT {@code realm_access.roles},
 * every {@code hasRole}/{@code hasAnyRole} check, and the frontend's {@code UserRole} type) rather than
 * inventing a second, inconsistent convention just for this one DTO.
 */
public enum UserRole {
    @JsonProperty("ROLE_ADMIN") ADMIN,
    @JsonProperty("ROLE_PMO") PMO,
    @JsonProperty("ROLE_POC") POC
}
