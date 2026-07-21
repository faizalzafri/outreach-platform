package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new notification template.
 */
@Schema(description = "Request payload for creating a new notification template")
public record TemplateCreateRequest(
        @Schema(description = "Template name", example = "feedback-reminder", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "Notification type", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull NotificationType type,
        @Schema(description = "Subject line template", example = "Feedback Request for {{eventName}}")
        @Size(max = 100) String subjectTemplate,
        @Schema(description = "Body template content", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String bodyTemplate,
        @Schema(description = "Template engine", example = "THYMELEAF", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull TemplateEngine engine,
        @Schema(description = "JSON schema for template variables")
        String variablesSchema
) {
}
