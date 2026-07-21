package com.outreach.platform.report.model;

import java.util.List;
import java.util.Map;

/**
 * Request body for updating an existing scheduled report configuration.
 * All fields are optional — only provided fields will be updated.
 */
public record ScheduledReportUpdateRequest(
        String name,
        String reportType,
        String cronExpression,
        ExportFormat exportFormat,
        Map<String, Object> filterCriteria,
        List<String> recipients,
        ScheduleStatus status
) {
}
