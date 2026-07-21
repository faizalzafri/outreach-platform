package com.outreach.platform.notification.model.dto;

import com.outreach.platform.notification.model.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Request payload for creating a new notification schedule.
 */
@Schema(description = "Request payload for creating a new notification schedule")
public record ScheduleCreateRequest(
        @Schema(description = "Template ID to use for sending", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID templateId,
        @Schema(description = "Event ID to scope the notification to")
        UUID eventId,
        @Schema(description = "Trigger type", example = "CRON", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull TriggerType triggerType,
        @Schema(description = "Cron expression for recurring schedules", example = "0 9 * * MON")
        @Size(max = 100) String cronExpression,
        @Schema(description = "One-time scheduled execution time")
        Instant scheduledAt,
        @Schema(description = "Recipient filter expression", example = "city:Bangalore")
        String recipientFilter
) {
}
