package com.outreach.platform.event.model.dto;

/**
 * Request to update a beneficiary's details.
 */
public record BeneficiaryUpdateRequest(
        String name,
        String organization,
        String contactEmail,
        String contactPhone,
        String city,
        String address,
        String description,
        Boolean active
) {
}
