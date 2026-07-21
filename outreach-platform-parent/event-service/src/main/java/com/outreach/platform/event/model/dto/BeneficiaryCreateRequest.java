package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to create a new beneficiary.
 */
@Schema(description = "Request payload for creating a new beneficiary organization")
public record BeneficiaryCreateRequest(
        @Schema(description = "Beneficiary name", example = "Green Earth Foundation", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String name,
        @Schema(description = "Organization name", example = "Green Earth NGO")
        String organization,
        @Schema(description = "Contact email", example = "contact@greenearth.org")
        String contactEmail,
        @Schema(description = "Contact phone", example = "+91-9876543210")
        String contactPhone,
        @Schema(description = "City", example = "Hyderabad")
        String city,
        @Schema(description = "Full address", example = "123 Park Avenue, Hyderabad")
        String address,
        @Schema(description = "Description", example = "Environmental conservation NGO")
        String description
) {
}
