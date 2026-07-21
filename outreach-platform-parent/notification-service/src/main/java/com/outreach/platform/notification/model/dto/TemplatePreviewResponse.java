package com.outreach.platform.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload containing a rendered template preview.
 */
@Schema(description = "Rendered template preview result")
public record TemplatePreviewResponse(
        @Schema(description = "Rendered subject line", example = "Feedback Request for Community Health Drive")
        String renderedSubject,
        @Schema(description = "Rendered email body")
        String renderedBody
) {
}
