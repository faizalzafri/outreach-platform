package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for transitioning an event's lifecycle status.
 */
public record StatusTransitionRequest(
        @NotNull EventStatus targetStatus
) {
}
