package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.UserRole;

import java.util.UUID;

/**
 * Read-only representation of a platform user.
 */
public record UserDto(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean enabled
) {
}
