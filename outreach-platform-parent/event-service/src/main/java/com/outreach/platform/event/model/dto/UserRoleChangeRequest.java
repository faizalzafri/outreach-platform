package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for changing a user's role.
 */
@Schema(description = "Request payload for changing a user's role")
public record UserRoleChangeRequest(

        @Schema(description = "New role to assign", example = "ROLE_ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UserRole role
) {
}
