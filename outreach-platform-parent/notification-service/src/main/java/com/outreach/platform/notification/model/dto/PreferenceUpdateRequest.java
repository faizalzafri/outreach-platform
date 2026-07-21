package com.outreach.platform.notification.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for updating notification preferences.
 */
public record PreferenceUpdateRequest(
        @NotNull Boolean emailEnabled,
        List<String> allowedTypes
) {
}
