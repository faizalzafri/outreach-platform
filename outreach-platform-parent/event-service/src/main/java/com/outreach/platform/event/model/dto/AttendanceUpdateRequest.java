package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Attendance for some of an event's enrolled volunteers. */
@Schema(description = "Attendance for some of an event's enrolled volunteers")
public record AttendanceUpdateRequest(
        @NotEmpty @Valid
        List<Entry> entries
) {

    @Schema(description = "One volunteer's attendance")
    public record Entry(
            @NotNull
            @Schema(description = "Volunteer ID")
            UUID volunteerId,
            @NotNull
            @Schema(description = "ATTENDED, NOT_ATTENDED, or REGISTERED to clear a mark", example = "ATTENDED")
            AttendanceStatus status
    ) {

        @AssertTrue(message = "status must be ATTENDED, NOT_ATTENDED or REGISTERED")
        @Schema(hidden = true)
        public boolean isMarkable() {
            return status != AttendanceStatus.UNREGISTERED;
        }
    }
}
