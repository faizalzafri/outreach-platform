package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model representing a notification template.
 */
public record TemplateDto(
        UUID id,
        String name,
        NotificationType type,
        String subjectTemplate,
        String bodyTemplate,
        TemplateEngine engine,
        String variablesSchema,
        boolean active,
        int version,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {
}
