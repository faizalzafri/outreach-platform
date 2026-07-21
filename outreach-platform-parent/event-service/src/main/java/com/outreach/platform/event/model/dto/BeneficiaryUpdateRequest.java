package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to update a beneficiary's details.
 */
@Schema(description = "Request payload for updating a beneficiary. All fields are optional.")
public record BeneficiaryUpdateRequest(
        @Schema(description = "Updated name", example = "Green Earth Foundation v2")
        String name,
        @Schema(description = "Updated organization", example = "Green Earth Trust")
        String organization,
        @Schema(description = "Updated contact email", example = "info@greenearth.org")
        String contactEmail,
        @Schema(description = "Updated contact phone", example = "+91-9876543211")
        String contactPhone,
        @Schema(description = "Updated city", example = "Chennai")
        String city,
        @Schema(description = "Updated address", example = "456 Lake Road, Chennai")
        String address,
        @Schema(description = "Updated description", example = "Environmental and social NGO")
        String description,
        @Schema(description = "Set active/inactive status", example = "true")
        Boolean active
) {
}
