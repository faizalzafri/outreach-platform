package com.outreach.platform.report.model;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request body for submitting an asynchronous report export.
 */
public record ExportRequest(

        @NotNull(message = "Export format is required")
        ExportFormat format,

        Map<String, Object> filters
) {
}
