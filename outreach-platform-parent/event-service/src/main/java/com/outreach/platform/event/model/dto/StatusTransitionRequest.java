package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for transitioning an event's lifecycle status.
 */
@Schema(description = "Request to transition an event's lifecycle status")
public record StatusTransitionRequest(
        @Schema(description = "Target lifecycle status", example = "PUBLISHED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull EventStatus targetStatus
) {
}
