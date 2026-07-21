package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to create a new beneficiary.
 */
public record BeneficiaryCreateRequest(
        @NotBlank String name,
        String organization,
        String contactEmail,
        String contactPhone,
        String city,
        String address,
        String description
) {
}
