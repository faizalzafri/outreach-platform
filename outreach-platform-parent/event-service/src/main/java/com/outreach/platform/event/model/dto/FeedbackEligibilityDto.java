package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** What feedback-service needs to decide whether feedback may be given for an event. */
@Schema(description = "An event's status and whether a given user is one of its POCs")
public record FeedbackEligibilityDto(
        @Schema(description = "Event ID")
        UUID eventId,
        @Schema(description = "Event lifecycle status", example = "ACTIVE")
        EventStatus status,
        @Schema(description = "Whether the asked-about user is assigned to the event as a POC")
        boolean assigned
) {
}
