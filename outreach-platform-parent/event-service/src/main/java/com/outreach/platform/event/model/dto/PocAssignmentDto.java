package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AssignmentRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only representation of a POC assignment.
 */
public record PocAssignmentDto(
        UUID id,
        UUID eventId,
        String eventName,
        UUID userId,
        String username,
        AssignmentRole assignmentRole,
        Instant assignedAt,
        String assignedBy
) {
}
