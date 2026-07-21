package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for enabling or disabling a user account.
 */
@Schema(description = "Request payload for enabling or disabling a user account")
public record UserStatusRequest(

        @Schema(description = "Whether the account should be enabled", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean enabled
) {
}
