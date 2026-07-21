package com.outreach.platform.ingestion.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Represents one row of parsed data from an Excel or CSV file.
 */
@Schema(description = "One row of parsed data from an Excel or CSV file")
public record ParsedRow(
        @Schema(description = "1-based row number in the source file", example = "5")
        int rowNumber,
        @Schema(description = "Column name to cell value mapping")
        Map<String, String> fields
) {
}
