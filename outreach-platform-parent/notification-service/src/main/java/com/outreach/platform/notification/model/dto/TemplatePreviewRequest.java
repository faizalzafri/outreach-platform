package com.outreach.platform.notification.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Request payload for previewing a rendered template with sample variables.
 */
public record TemplatePreviewRequest(
        @NotNull UUID templateId,
        @NotNull Map<String, Object> variables
) {
}
