package com.outreach.platform.event.model.dto;

import java.util.UUID;

/**
 * Read-only representation of a beneficiary organization.
 */
public record BeneficiaryDto(
        UUID id,
        String name,
        String organization,
        String contactEmail,
        String contactPhone,
        String city,
        String address,
        String description,
        boolean active
) {
}
