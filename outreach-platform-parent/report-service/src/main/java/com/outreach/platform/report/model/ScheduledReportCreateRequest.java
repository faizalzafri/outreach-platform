package com.outreach.platform.report.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request body for creating a new scheduled report.
 */
public record ScheduledReportCreateRequest(

        @NotBlank(message = "Report name is required")
        String name,

        @NotBlank(message = "Report type is required")
        String reportType,

        @NotBlank(message = "Cron expression is required")
        String cronExpression,

        @NotNull(message = "Export format is required")
        ExportFormat exportFormat,

        Map<String, Object> filterCriteria,

        @NotEmpty(message = "At least one recipient is required")
        List<String> recipients
) {
}
