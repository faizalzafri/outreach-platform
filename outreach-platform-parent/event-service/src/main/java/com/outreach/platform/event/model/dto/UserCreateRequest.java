package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new user account.
 */
@Schema(description = "Request payload for creating a new user account")
public record UserCreateRequest(

        @Schema(description = "Username (3-100 characters)", example = "john.doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 3, max = 100)
        String username,

        @Schema(description = "Email address", example = "john.doe@company.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        String email,

        @Schema(description = "Password (8-128 characters)", example = "P@ssw0rd123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @Schema(description = "User role", example = "ROLE_PMO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        UserRole role
) {
}
