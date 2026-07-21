package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.VolunteerAvailability;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only representation of a volunteer profile.
 */
@Schema(description = "Read-only representation of a volunteer profile")
public record VolunteerDto(
        @Schema(description = "Volunteer unique identifier")
        UUID id,
        @Schema(description = "Corporate employee ID", example = "EMP-1234")
        String employeeId,
        @Schema(description = "Volunteer full name", example = "Rajesh Kumar")
        String fullName,
        @Schema(description = "Email address", example = "rajesh.kumar@company.com")
        String email,
        @Schema(description = "Phone number", example = "+91-9876543210")
        String phone,
        @Schema(description = "Base city/location", example = "Bangalore")
        String baseLocation,
        @Schema(description = "Department", example = "Engineering")
        String department,
        @Schema(description = "Job designation", example = "Senior Developer")
        String designation,
        @Schema(description = "Comma-separated skills", example = "teaching,first-aid,cooking")
        String skills,
        @Schema(description = "Availability status", example = "AVAILABLE")
        VolunteerAvailability availability,
        @Schema(description = "Total number of events participated in", example = "12")
        Integer totalEventsParticipated,
        @Schema(description = "Average feedback score received", example = "4.5")
        BigDecimal avgFeedbackScore
) {
}
