package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for adding a user to a team.
 */
public record AddTeamMemberRequest(

        @NotNull(message = "User ID is required")
        UUID userId
) {
}
