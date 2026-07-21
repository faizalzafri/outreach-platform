package com.outreach.platform.ingestion.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Aggregated result of parsing and validating a file.
 */
@Schema(description = "Aggregated result of parsing and validating an uploaded file")
public record ParseResult(
        @Schema(description = "Rows that passed all validation rules")
        List<ParsedRow> validRows,
        @Schema(description = "Validation errors (one per invalid cell)")
        List<ValidationError> errors,
        @Schema(description = "Total data rows encountered (excluding header)", example = "150")
        int totalRows,
        @Schema(description = "Number of rows with no validation errors", example = "145")
        int validCount,
        @Schema(description = "Number of rows with at least one validation error", example = "5")
        int errorCount
) {
}
