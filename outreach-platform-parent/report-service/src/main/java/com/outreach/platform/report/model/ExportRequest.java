package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request body for submitting an asynchronous report export.
 */
@Schema(description = "Request body for submitting an asynchronous report export")
public record ExportRequest(

        @Schema(description = "Export format", example = "PDF", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Export format is required")
        ExportFormat format,

        @Schema(description = "Filter criteria for the export")
        Map<String, Object> filters
) {
}
