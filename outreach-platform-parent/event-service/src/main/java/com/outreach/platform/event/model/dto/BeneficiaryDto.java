package com.outreach.platform.event.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Read-only representation of a beneficiary organization.
 */
@Schema(description = "Read-only representation of a beneficiary organization")
public record BeneficiaryDto(
        @Schema(description = "Beneficiary unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Beneficiary name", example = "Green Earth Foundation")
        String name,
        @Schema(description = "Organization name", example = "Green Earth NGO")
        String organization,
        @Schema(description = "Contact email address", example = "contact@greenearth.org")
        String contactEmail,
        @Schema(description = "Contact phone number", example = "+91-9876543210")
        String contactPhone,
        @Schema(description = "City", example = "Hyderabad")
        String city,
        @Schema(description = "Full address", example = "123 Park Avenue, Hyderabad")
        String address,
        @Schema(description = "Description of the beneficiary", example = "Environmental conservation NGO")
        String description,
        @Schema(description = "Whether the beneficiary is active", example = "true")
        boolean active
) {
}
