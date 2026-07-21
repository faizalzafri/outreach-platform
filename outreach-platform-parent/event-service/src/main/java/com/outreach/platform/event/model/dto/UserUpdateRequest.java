package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing user account.
 */
public record UserUpdateRequest(

        @Size(min = 3, max = 100)
        String username,

        @Email
        String email,

        @Size(min = 8, max = 128)
        String password
) {
}
