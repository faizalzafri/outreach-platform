package com.outreach.platform.auth.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SelectTenantRequest(
        @NotNull UUID tenantId
) {
}
