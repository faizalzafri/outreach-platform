package com.outreach.platform.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Request payload for previewing a rendered template with sample variables.
 */
@Schema(description = "Request to preview a rendered notification template with sample variables")
public record TemplatePreviewRequest(
        @Schema(description = "Template ID to preview", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID templateId,
        @Schema(description = "Sample variables to render the template with", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Map<String, Object> variables
) {
}
