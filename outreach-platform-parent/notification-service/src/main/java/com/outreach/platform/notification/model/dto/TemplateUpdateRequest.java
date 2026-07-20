package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing notification template.
 * All fields are optional — only non-null values are applied.
 */
public record TemplateUpdateRequest(
        @Size(max = 100) String name,
        NotificationType type,
        @Size(max = 100) String subjectTemplate,
        String bodyTemplate,
        TemplateEngine engine,
        String variablesSchema,
        Boolean active
) {
}
