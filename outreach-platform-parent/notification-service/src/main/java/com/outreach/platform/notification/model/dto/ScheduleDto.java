package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.ScheduleStatus;
import com.outreach.platform.notification.model.TriggerType;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model representing a notification schedule.
 */
public record ScheduleDto(
        UUID id,
        UUID templateId,
        UUID eventId,
        TriggerType triggerType,
        String cronExpression,
        Instant scheduledAt,
        ScheduleStatus status,
        String recipientFilter,
        Instant createdAt,
        String createdBy
) {
}
