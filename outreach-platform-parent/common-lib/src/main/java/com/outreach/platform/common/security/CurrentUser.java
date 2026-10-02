package com.outreach.platform.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Who is calling. The token's subject is the username; the user's id travels in the {@code uid}
 * claim, which is what user ids elsewhere (POC assignments, team memberships, ...) refer to.
 */
public final class CurrentUser {

    private static final Set<String> MANAGER_ROLES =
            Set.of("ROLE_PMO", "ROLE_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_PLATFORM_ADMIN");

    private CurrentUser() {
    }

    /** The caller's user id; empty for service-to-service tokens and anonymous calls. */
    public static Optional<UUID> id() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            String uid = token.getToken().getClaimAsString("uid");
            if (uid != null) {
                return Optional.of(UUID.fromString(uid));
            }
        }
        return Optional.empty();
    }

    /** The caller's user id, or {@link IllegalStateException} when the call is not from a user. */
    public static UUID requireId() {
        return id().orElseThrow(() -> new IllegalStateException("No authenticated user on this request"));
    }

    /** Whether the caller holds any of the given roles (without the {@code ROLE_} prefix). */
    public static boolean hasAnyRole(String... roles) {
        Set<String> held = authorities();
        return Arrays.stream(roles).anyMatch(role -> held.contains("ROLE_" + role));
    }

    /**
     * A POC with no managing role (PMO, ADMIN, TENANT_ADMIN, PLATFORM_ADMIN): sees and acts on
     * only the events they are assigned to.
     */
    public static boolean isPocOnly() {
        Set<String> held = authorities();
        return held.contains("ROLE_POC") && held.stream().noneMatch(MANAGER_ROLES::contains);
    }

    private static Set<String> authorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
