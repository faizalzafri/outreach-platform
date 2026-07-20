package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new notification template.
 */
public record TemplateCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull NotificationType type,
        @Size(max = 100) String subjectTemplate,
        @NotBlank String bodyTemplate,
        @NotNull TemplateEngine engine,
        String variablesSchema
) {
}
