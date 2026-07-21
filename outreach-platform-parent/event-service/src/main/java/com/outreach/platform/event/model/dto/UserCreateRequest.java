package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new user account.
 */
public record UserCreateRequest(

        @NotBlank
        @Size(min = 3, max = 100)
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @NotNull
        UserRole role
) {
}
