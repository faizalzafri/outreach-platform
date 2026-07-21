package com.outreach.platform.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO representing an export job's current state.
 */
@Schema(description = "Export job status and metadata")
public record ExportJobDto(
        @Schema(description = "Unique job identifier", example = "6614a1b2c3d4e5f6a7b8c9d0")
        String jobId,
        @Schema(description = "Current job status", example = "COMPLETED")
        ExportJobStatus status,
        @Schema(description = "Export format", example = "PDF")
        ExportFormat format,
        @Schema(description = "Generated file name", example = "report-2024-06.pdf")
        String fileName,
        @Schema(description = "Error message if failed")
        String errorMessage,
        @Schema(description = "Job creation timestamp")
        Instant createdAt,
        @Schema(description = "Job completion timestamp")
        Instant completedAt
) {
}
