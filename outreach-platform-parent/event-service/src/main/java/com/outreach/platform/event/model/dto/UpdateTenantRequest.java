package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing tenant.
 */
public record UpdateTenantRequest(

        @NotBlank(message = "Tenant name is required")
        @Size(min = 3, max = 100, message = "Tenant name must be between 3 and 100 characters")
        String name
) {
}
