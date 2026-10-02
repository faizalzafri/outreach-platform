package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AccessLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** A team that has access to an event. */
@Schema(description = "A team that has access to an event")
public record TeamAccessDto(
        @Schema(description = "Team ID")
        UUID teamId,
        @Schema(description = "Team name", example = "Mumbai Volunteers")
        String teamName,
        @Schema(description = "VIEW: see the event and give feedback; EDIT: also record attendance", example = "VIEW")
        AccessLevel accessLevel
) {
}
