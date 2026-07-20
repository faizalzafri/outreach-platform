package com.outreach.platform.ingestion.model;

import java.util.List;

/**
 * Result of a dry-run validation (file parsed and validated without importing).
 *
 * @param fileName   original file name
 * @param totalRows  total data rows parsed
 * @param validCount rows that passed validation
 * @param errorCount rows with at least one error
 * @param errors     detailed per-cell errors
 */
public record ValidationResult(
        String fileName,
        int totalRows,
        int validCount,
        int errorCount,
        List<ValidationError> errors
) {
}
