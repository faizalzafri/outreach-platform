package com.outreach.platform.ingestion.model;

import java.util.List;

/**
 * Aggregated result of parsing and validating a file.
 *
 * @param validRows  rows that passed all validation rules
 * @param errors     validation errors (one per invalid cell)
 * @param totalRows  total data rows encountered (excluding header)
 * @param validCount number of rows with no validation errors
 * @param errorCount number of rows with at least one validation error
 */
public record ParseResult(
        List<ParsedRow> validRows,
        List<ValidationError> errors,
        int totalRows,
        int validCount,
        int errorCount
) {
}
