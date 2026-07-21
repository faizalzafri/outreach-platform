package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Representation of system configuration properties that are viewable and updatable by admins.
 */
@Schema(description = "System configuration properties viewable and updatable by admins")
public record SystemConfigDto(
        @Schema(description = "Maximum events per page", example = "50")
        int maxEventsPerPage,
        @Schema(description = "Default pagination page size", example = "20")
        int defaultPageSize,
        @Schema(description = "Prefix for auto-generated event codes", example = "EVT")
        String eventCodePrefix,
        @Schema(description = "Additional configurable properties")
        Map<String, String> additionalProperties
) {
}
