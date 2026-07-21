package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AssignmentRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to assign a POC user to an event.
 */
@Schema(description = "Request to assign a Point of Contact to an event")
public record PocAssignRequest(
        @Schema(description = "User ID to assign as POC", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID userId,
        @Schema(description = "Assignment role", example = "PRIMARY", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull AssignmentRole role
) {
}
