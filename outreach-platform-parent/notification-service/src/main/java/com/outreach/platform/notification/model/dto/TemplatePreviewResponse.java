package com.outreach.platform.notification.model.dto;

/**
 * Response payload containing a rendered template preview.
 */
public record TemplatePreviewResponse(
        String renderedSubject,
        String renderedBody
) {
}
