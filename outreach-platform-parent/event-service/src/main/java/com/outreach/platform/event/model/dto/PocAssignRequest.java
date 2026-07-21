package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AssignmentRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to assign a POC user to an event.
 */
public record PocAssignRequest(
        @NotNull UUID userId,
        @NotNull AssignmentRole role
) {
}
