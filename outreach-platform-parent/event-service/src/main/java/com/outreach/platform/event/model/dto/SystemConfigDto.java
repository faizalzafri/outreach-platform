package com.outreach.platform.event.model.dto;

import java.util.Map;

/**
 * Representation of system configuration properties that are viewable and updatable by admins.
 */
public record SystemConfigDto(
        int maxEventsPerPage,
        int defaultPageSize,
        String eventCodePrefix,
        Map<String, String> additionalProperties
) {
}
