package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.ScheduleStatus;
import com.outreach.platform.notification.model.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model representing a notification schedule.
 */
@Schema(description = "Read-only representation of a notification schedule")
public record ScheduleDto(
        @Schema(description = "Schedule unique identifier")
        UUID id,
        @Schema(description = "Template ID used for sending")
        UUID templateId,
        @Schema(description = "Event ID scope")
        UUID eventId,
        @Schema(description = "Trigger type", example = "CRON")
        TriggerType triggerType,
        @Schema(description = "Cron expression", example = "0 9 * * MON")
        String cronExpression,
        @Schema(description = "One-time scheduled time")
        Instant scheduledAt,
        @Schema(description = "Current schedule status", example = "ACTIVE")
        ScheduleStatus status,
        @Schema(description = "Recipient filter expression", example = "city:Bangalore")
        String recipientFilter,
        @Schema(description = "Creation timestamp")
        Instant createdAt,
        @Schema(description = "Creator username", example = "admin")
        String createdBy
) {
}
