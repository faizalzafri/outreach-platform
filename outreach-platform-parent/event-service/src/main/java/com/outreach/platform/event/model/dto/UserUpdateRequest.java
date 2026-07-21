package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing user account.
 */
@Schema(description = "Request payload for updating an existing user. All fields are optional.")
public record UserUpdateRequest(

        @Schema(description = "Updated username", example = "john.doe.updated")
        @Size(min = 3, max = 100)
        String username,

        @Schema(description = "Updated email", example = "john.updated@company.com")
        @Email
        String email,

        @Schema(description = "Updated password", example = "N3wP@ssw0rd!")
        @Size(min = 8, max = 128)
        String password
) {
}
