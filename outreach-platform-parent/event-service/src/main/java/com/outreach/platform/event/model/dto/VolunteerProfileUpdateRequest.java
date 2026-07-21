package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.VolunteerAvailability;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to update a volunteer's profile.
 */
@Schema(description = "Request to update a volunteer's profile. All fields are optional.")
public record VolunteerProfileUpdateRequest(
        @Schema(description = "Updated full name", example = "Rajesh Kumar Singh")
        String fullName,
        @Schema(description = "Updated email", example = "rajesh.singh@company.com")
        String email,
        @Schema(description = "Updated phone", example = "+91-9876543211")
        String phone,
        @Schema(description = "Updated base location", example = "Mumbai")
        String baseLocation,
        @Schema(description = "Updated department", example = "Product")
        String department,
        @Schema(description = "Updated designation", example = "Staff Engineer")
        String designation,
        @Schema(description = "Updated skills", example = "mentoring,public-speaking")
        String skills,
        @Schema(description = "Updated availability", example = "BUSY")
        VolunteerAvailability availability
) {
}
