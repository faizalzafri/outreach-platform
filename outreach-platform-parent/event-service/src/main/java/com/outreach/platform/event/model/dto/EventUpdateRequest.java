package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for updating an existing event.
 */
@Schema(description = "Request payload for updating an existing event. All fields are optional.")
public record EventUpdateRequest(
        @Schema(description = "Updated event name", example = "Community Health Drive 2024")
        @Size(max = 255) String eventName,
        @Schema(description = "Updated description", example = "Revised health awareness program")
        String description,
        @Schema(description = "Updated start date", example = "2024-07-01")
        LocalDate eventDate,
        @Schema(description = "Updated end date", example = "2024-07-03")
        LocalDate eventEndDate,
        @Schema(description = "Updated city", example = "Mumbai")
        @Size(max = 100) String city,
        @Schema(description = "Updated venue", example = "Town Hall")
        @Size(max = 255) String venue,
        @Schema(description = "Updated category", example = "Education")
        @Size(max = 50) String category,
        @Schema(description = "Updated max volunteer capacity", example = "75")
        Integer maxVolunteers
) {
}
