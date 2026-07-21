package com.outreach.platform.ingestion.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Result of a dry-run validation (file parsed and validated without importing).
 */
@Schema(description = "Result of a dry-run validation — file parsed and validated without importing")
public record ValidationResult(
        @Schema(description = "Original file name", example = "volunteers-march-2024.xlsx")
        String fileName,
        @Schema(description = "Total data rows parsed", example = "150")
        int totalRows,
        @Schema(description = "Rows that passed validation", example = "145")
        int validCount,
        @Schema(description = "Rows with at least one error", example = "5")
        int errorCount,
        @Schema(description = "Detailed per-cell validation errors")
        List<ValidationError> errors
) {
}
