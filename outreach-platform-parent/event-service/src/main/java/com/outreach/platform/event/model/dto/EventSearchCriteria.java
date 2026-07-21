package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;

import java.time.LocalDate;

/**
 * Search criteria for filtering events with pagination support.
 */
public record EventSearchCriteria(
        EventStatus status,
        String city,
        String category,
        LocalDate dateFrom,
        LocalDate dateTo,
        String query
) {
}
