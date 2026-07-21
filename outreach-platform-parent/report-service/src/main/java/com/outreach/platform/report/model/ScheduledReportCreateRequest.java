package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request body for creating a new scheduled report.
 */
@Schema(description = "Request body for creating a new scheduled report")
public record ScheduledReportCreateRequest(

        @Schema(description = "Report name", example = "Weekly Feedback Summary", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Report name is required")
        String name,

        @Schema(description = "Report type", example = "FEEDBACK_SUMMARY", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Report type is required")
        String reportType,

        @Schema(description = "Cron expression for scheduling", example = "0 0 9 * * MON", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Cron expression is required")
        String cronExpression,

        @Schema(description = "Export format for the report", example = "PDF", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Export format is required")
        ExportFormat exportFormat,

        @Schema(description = "Filter criteria for the report")
        Map<String, Object> filterCriteria,

        @Schema(description = "Email recipients", example = "[\"admin@company.com\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "At least one recipient is required")
        List<String> recipients
) {
}
