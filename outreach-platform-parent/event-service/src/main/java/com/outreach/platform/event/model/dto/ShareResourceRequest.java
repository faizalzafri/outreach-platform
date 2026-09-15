package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.PermissionLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for sharing a resource with a team.
 */
public record ShareResourceRequest(

        @NotNull(message = "Team ID is required")
        UUID teamId,

        @NotNull(message = "Permission level is required")
        PermissionLevel permissionLevel
) {
}
