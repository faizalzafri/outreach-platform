package com.outreach.platform.report.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a scheduled report configuration.
 */
public record ScheduledReportDto(
        UUID id,
        String name,
        String reportType,
        String cronExpression,
        ExportFormat exportFormat,
        Map<String, Object> filterCriteria,
        List<String> recipients,
        ScheduleStatus status,
        Instant nextRunAt,
        Instant createdAt
) {
}
