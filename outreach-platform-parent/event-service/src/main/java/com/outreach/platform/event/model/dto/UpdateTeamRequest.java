package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing team's name and description.
 */
public record UpdateTeamRequest(

        @NotBlank(message = "Team name is required")
        @Size(min = 1, max = 100, message = "Team name must be between 1 and 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
