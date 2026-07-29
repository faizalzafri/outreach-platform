package com.outreach.platform.event.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for the tenant onboarding endpoint.
 * Creates a tenant and its first admin membership in a single atomic operation.
 */
public record OnboardTenantRequest(

        @NotBlank(message = "Tenant name is required")
        @Size(min = 3, max = 100, message = "Tenant name must be between 3 and 100 characters")
        String tenantName,

        @NotBlank(message = "Tenant slug is required")
        @Size(min = 3, max = 50, message = "Tenant slug must be between 3 and 50 characters")
        @Pattern(
                regexp = "^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$",
                message = "Slug must contain only lowercase letters, digits, and hyphens; must start and end with a letter or digit"
        )
        String tenantSlug,

        @NotNull(message = "Admin user ID is required")
        UUID adminUserId
) {
}
