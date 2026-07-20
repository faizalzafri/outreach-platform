package com.outreach.platform.ingestion.model;

import java.util.Map;

/**
 * Represents one row of parsed data from an Excel or CSV file.
 *
 * @param rowNumber  the 1-based row number in the source file
 * @param fields     column name to cell value mapping
 */
public record ParsedRow(
        int rowNumber,
        Map<String, String> fields
) {
}
