package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Search criteria for filtering events with pagination support.
 */
@Schema(description = "Search criteria for filtering events")
public record EventSearchCriteria(
        @Schema(description = "Filter by lifecycle status", example = "ACTIVE")
        EventStatus status,
        @Schema(description = "Filter by city", example = "Bangalore")
        String city,
        @Schema(description = "Filter by category", example = "Health")
        String category,
        @Schema(description = "Start of date range filter", example = "2024-01-01")
        LocalDate dateFrom,
        @Schema(description = "End of date range filter", example = "2024-12-31")
        LocalDate dateTo,
        @Schema(description = "Full-text search query", example = "health drive")
        String query
) {
}
