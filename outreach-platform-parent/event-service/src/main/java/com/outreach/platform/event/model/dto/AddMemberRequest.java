package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.TenantRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for adding a member to a tenant.
 */
public record AddMemberRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Role is required")
        TenantRole role
) {
}
