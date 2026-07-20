package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.TriggerType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Request payload for creating a new notification schedule.
 */
public record ScheduleCreateRequest(
        @NotNull UUID templateId,
        UUID eventId,
        @NotNull TriggerType triggerType,
        @Size(max = 100) String cronExpression,
        Instant scheduledAt,
        String recipientFilter
) {
}
