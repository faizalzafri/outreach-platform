package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing notification template (all fields optional).
 */
@Schema(description = "Request payload for updating a notification template. All fields are optional.")
public record TemplateUpdateRequest(
        @Schema(description = "Updated template name", example = "feedback-reminder-v2")
        @Size(max = 100) String name,
        @Schema(description = "Updated notification type", example = "EMAIL")
        NotificationType type,
        @Schema(description = "Updated subject template", example = "Updated: Feedback for {{eventName}}")
        @Size(max = 100) String subjectTemplate,
        @Schema(description = "Updated body template")
        String bodyTemplate,
        @Schema(description = "Updated template engine", example = "THYMELEAF")
        TemplateEngine engine,
        @Schema(description = "Updated variables schema")
        String variablesSchema,
        @Schema(description = "Activate or deactivate the template", example = "true")
        Boolean active
) {
}
