package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AssignmentRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only representation of a POC assignment.
 */
@Schema(description = "Read-only representation of a Point of Contact (POC) assignment to an event")
public record PocAssignmentDto(
        @Schema(description = "Assignment unique identifier")
        UUID id,
        @Schema(description = "Event ID the POC is assigned to")
        UUID eventId,
        @Schema(description = "Event name", example = "Community Health Drive")
        String eventName,
        @Schema(description = "Assigned user ID")
        UUID userId,
        @Schema(description = "Assigned user's username", example = "john.doe")
        String username,
        @Schema(description = "Role in the assignment", example = "PRIMARY")
        AssignmentRole assignmentRole,
        @Schema(description = "Timestamp of assignment")
        Instant assignedAt,
        @Schema(description = "Username who made the assignment", example = "admin")
        String assignedBy
) {
}
