package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Request body for updating an existing scheduled report configuration.
 * All fields are optional — only provided fields will be updated.
 */
@Schema(description = "Request for updating a scheduled report. All fields are optional.")
public record ScheduledReportUpdateRequest(
        @Schema(description = "Updated report name", example = "Monthly Feedback Summary")
        String name,
        @Schema(description = "Updated report type", example = "NPS_REPORT")
        String reportType,
        @Schema(description = "Updated cron expression", example = "0 0 9 1 * *")
        String cronExpression,
        @Schema(description = "Updated export format", example = "EXCEL")
        ExportFormat exportFormat,
        @Schema(description = "Updated filter criteria")
        Map<String, Object> filterCriteria,
        @Schema(description = "Updated recipients list")
        List<String> recipients,
        @Schema(description = "Updated schedule status", example = "PAUSED")
        ScheduleStatus status
) {
}
