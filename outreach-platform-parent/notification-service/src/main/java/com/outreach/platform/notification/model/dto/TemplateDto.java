package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model representing a notification template.
 */
@Schema(description = "Read-only representation of a notification template")
public record TemplateDto(
        @Schema(description = "Template unique identifier")
        UUID id,
        @Schema(description = "Template name", example = "feedback-reminder")
        String name,
        @Schema(description = "Notification type", example = "EMAIL")
        NotificationType type,
        @Schema(description = "Subject line template", example = "Feedback Request for {{eventName}}")
        String subjectTemplate,
        @Schema(description = "Body template content")
        String bodyTemplate,
        @Schema(description = "Template engine used", example = "THYMELEAF")
        TemplateEngine engine,
        @Schema(description = "JSON schema defining available template variables")
        String variablesSchema,
        @Schema(description = "Whether the template is active", example = "true")
        boolean active,
        @Schema(description = "Template version number", example = "2")
        long version,
        @Schema(description = "Creation timestamp")
        Instant createdAt,
        @Schema(description = "Last update timestamp")
        Instant updatedAt,
        @Schema(description = "Creator username", example = "admin")
        String createdBy
) {
}
