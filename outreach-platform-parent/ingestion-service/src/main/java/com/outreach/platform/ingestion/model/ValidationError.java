package com.outreach.platform.ingestion.model;

/**
 * Represents a validation error for a specific cell in a parsed file.
 *
 * @param rowNumber     the 1-based row number where the error occurred
 * @param columnName    the column/header name of the invalid cell
 * @param errorMessage  human-readable error description
 * @param rejectedValue the value that failed validation (may be null or blank)
 */
public record ValidationError(
        int rowNumber,
        String columnName,
        String errorMessage,
        String rejectedValue
) {
}
