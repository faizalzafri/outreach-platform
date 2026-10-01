package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.service.TenantAdminService;
import jakarta.inject.Inject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Lets the gateway learn a tenant's status on a cache miss, so a suspended or deactivated tenant
 * is refused at the edge. Not routed through the gateway; an unknown id reads as DEACTIVATED.
 */
// ponytail: unauthenticated because it only reveals a status for a UUID; put it behind a
// client-credentials token if auth-service's port stops being private.
@RestController
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class InternalTenantStatusController {

    private final TenantAdminService tenants;

    @Inject
    public InternalTenantStatusController(TenantAdminService tenants) {
        this.tenants = tenants;
    }

    @GetMapping("/internal/tenants/{id}/status")
    public Map<String, String> status(@PathVariable UUID id) {
        return Map.of("status", tenants.status(id).name());
    }
}
