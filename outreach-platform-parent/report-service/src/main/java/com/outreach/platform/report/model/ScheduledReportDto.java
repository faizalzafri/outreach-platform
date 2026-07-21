package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a scheduled report configuration.
 */
@Schema(description = "Scheduled report configuration and status")
public record ScheduledReportDto(
        @Schema(description = "Schedule unique identifier")
        UUID id,
        @Schema(description = "Report name", example = "Weekly Feedback Summary")
        String name,
        @Schema(description = "Report type", example = "FEEDBACK_SUMMARY")
        String reportType,
        @Schema(description = "Cron expression", example = "0 0 9 * * MON")
        String cronExpression,
        @Schema(description = "Export format", example = "PDF")
        ExportFormat exportFormat,
        @Schema(description = "Filter criteria")
        Map<String, Object> filterCriteria,
        @Schema(description = "Email recipients")
        List<String> recipients,
        @Schema(description = "Schedule status", example = "ACTIVE")
        ScheduleStatus status,
        @Schema(description = "Next scheduled run time")
        Instant nextRunAt,
        @Schema(description = "Creation timestamp")
        Instant createdAt
) {
}
