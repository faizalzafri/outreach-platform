package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for changing a user's role.
 */
public record UserRoleChangeRequest(

        @NotNull
        UserRole role
) {
}
