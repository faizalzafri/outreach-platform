package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to upsert a volunteer profile and enroll it in an event, in one call.
 * Used by ingestion-service's bulk file-import pipeline.
 */
@Schema(description = "Upserts a volunteer profile by employeeId and enrolls it in the event identified by eventCode")
public record VolunteerImportRequest(
        @NotBlank
        @Schema(description = "Employee ID", example = "EMP001")
        String employeeId,
        @NotBlank
        @Schema(description = "Full name", example = "Rajesh Kumar Singh")
        String fullName,
        @NotBlank
        @Schema(description = "Email", example = "rajesh.singh@company.com")
        String email,
        @Schema(description = "Phone", example = "+91-9876543211")
        String phone,
        @Schema(description = "Base location", example = "Mumbai")
        String baseLocation,
        @Schema(description = "Department", example = "Engineering")
        String department,
        @Schema(description = "Designation", example = "Staff Engineer")
        String designation,
        @Schema(description = "Comma-separated skills", example = "Java,Mentoring")
        String skills,
        @NotBlank
        @Schema(description = "Event code to enroll the volunteer in", example = "EVT-2024-003")
        String eventCode
) {
}
