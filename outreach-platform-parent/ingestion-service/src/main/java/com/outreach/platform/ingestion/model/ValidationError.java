package com.outreach.platform.ingestion.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a validation error for a specific cell in a parsed file.
 */
@Schema(description = "Validation error for a specific cell in a parsed file")
public record ValidationError(
        @Schema(description = "1-based row number where the error occurred", example = "12")
        int rowNumber,
        @Schema(description = "Column/header name of the invalid cell", example = "email")
        String columnName,
        @Schema(description = "Human-readable error description", example = "Invalid email format")
        String errorMessage,
        @Schema(description = "The value that failed validation", example = "not-an-email")
        String rejectedValue
) {
}
