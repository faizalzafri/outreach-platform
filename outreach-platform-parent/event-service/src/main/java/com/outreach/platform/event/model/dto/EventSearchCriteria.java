package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

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
        String query,
        @Schema(description = "Only events this POC works on, directly or through a shared team")
        UUID pocId
) {

    public EventSearchCriteria(EventStatus status, String city, String category, LocalDate dateFrom, LocalDate dateTo,
                               String query) {
        this(status, city, category, dateFrom, dateTo, query, null);
    }
}
