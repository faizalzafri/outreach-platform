package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for creating a new event.
 */
@Schema(description = "Request payload for creating a new outreach event")
public record EventCreateRequest(
        @Schema(description = "Event display name", example = "Community Health Drive", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 255) String eventName,
        @Schema(description = "Detailed event description", example = "Annual health awareness camp")
        String description,
        @Schema(description = "Event start date", example = "2024-06-15")
        LocalDate eventDate,
        @Schema(description = "Event end date", example = "2024-06-17")
        LocalDate eventEndDate,
        @Schema(description = "City where the event takes place", example = "Bangalore")
        @Size(max = 100) String city,
        @Schema(description = "Venue name or address", example = "Convention Center, MG Road")
        @Size(max = 255) String venue,
        @Schema(description = "Event category", example = "Health")
        @Size(max = 50) String category,
        @Schema(description = "Maximum volunteer capacity", example = "50")
        Integer maxVolunteers
) {
}
