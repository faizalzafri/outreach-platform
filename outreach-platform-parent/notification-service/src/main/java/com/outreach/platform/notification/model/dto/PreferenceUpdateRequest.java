package com.outreach.platform.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for updating notification preferences.
 */
@Schema(description = "Request payload for updating notification preferences")
public record PreferenceUpdateRequest(
        @Schema(description = "Enable or disable email notifications", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean emailEnabled,
        @Schema(description = "Allowed notification types", example = "[\"EMAIL\"]")
        List<String> allowedTypes
) {
}
