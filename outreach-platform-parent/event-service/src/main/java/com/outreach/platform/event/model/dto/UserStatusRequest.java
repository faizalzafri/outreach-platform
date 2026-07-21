package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for enabling or disabling a user account.
 */
public record UserStatusRequest(

        @NotNull
        Boolean enabled
) {
}
