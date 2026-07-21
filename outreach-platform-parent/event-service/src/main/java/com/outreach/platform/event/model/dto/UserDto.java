package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Read-only representation of a platform user.
 */
@Schema(description = "Read-only representation of a platform user account")
public record UserDto(
        @Schema(description = "User unique identifier")
        UUID id,
        @Schema(description = "Username", example = "john.doe")
        String username,
        @Schema(description = "Email address", example = "john.doe@company.com")
        String email,
        @Schema(description = "Assigned role", example = "ROLE_PMO")
        UserRole role,
        @Schema(description = "Whether the account is enabled", example = "true")
        boolean enabled
) {
}
