package com.outreach.platform.report.model;

import java.time.Instant;

/**
 * Response DTO representing an export job's current state.
 */
public record ExportJobDto(
        String jobId,
        ExportJobStatus status,
        ExportFormat format,
        String fileName,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {
}
