package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AccessLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** The access a team gets to an event. */
@Schema(description = "The access a team gets to an event")
public record TeamAccessRequest(
        @NotNull
        @Schema(description = "VIEW or EDIT", example = "EDIT")
        AccessLevel accessLevel
) {
}
