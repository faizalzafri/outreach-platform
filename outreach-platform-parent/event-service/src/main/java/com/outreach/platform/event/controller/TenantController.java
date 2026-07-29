package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.CreateTenantRequest;
import com.outreach.platform.event.model.dto.OnboardTenantRequest;
import com.outreach.platform.event.model.dto.OnboardTenantResponse;
import com.outreach.platform.event.model.dto.TenantResponse;
import com.outreach.platform.event.model.dto.UpdateTenantRequest;
import com.outreach.platform.event.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for tenant CRUD operations and lifecycle management.
 * All endpoints require PLATFORM_ADMIN role.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Tenant Management", description = "Create, read, update, and manage tenant lifecycle")
public class TenantController {

    private final TenantService tenantService;

    @Inject
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(summary = "Create tenant", description = "Creates a new tenant with ACTIVE status")
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse created = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List tenants", description = "Returns a paginated list of all tenants")
    @GetMapping
    public ResponseEntity<Page<TenantResponse>> listTenants(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20)")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<TenantResponse> tenants = tenantService.listTenants(pageable);
        return ResponseEntity.ok(tenants);
    }

    @Operation(summary = "Get tenant", description = "Returns a single tenant by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenant(
            @Parameter(description = "Tenant UUID") @PathVariable UUID id) {
        TenantResponse tenant = tenantService.getTenant(id);
        return ResponseEntity.ok(tenant);
    }

    @Operation(summary = "Update tenant", description = "Updates an existing tenant's name")
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponse> updateTenant(
            @Parameter(description = "Tenant UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantResponse updated = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Deactivate tenant", description = "Sets tenant status to DEACTIVATED")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<TenantResponse> deactivateTenant(
            @Parameter(description = "Tenant UUID") @PathVariable UUID id) {
        TenantResponse deactivated = tenantService.deactivateTenant(id);
        return ResponseEntity.ok(deactivated);
    }

    @Operation(summary = "Suspend tenant", description = "Sets tenant status to SUSPENDED")
    @PostMapping("/{id}/suspend")
    public ResponseEntity<TenantResponse> suspendTenant(
            @Parameter(description = "Tenant UUID") @PathVariable UUID id) {
        TenantResponse suspended = tenantService.suspendTenant(id);
        return ResponseEntity.ok(suspended);
    }

    @Operation(summary = "Activate tenant", description = "Reactivates a suspended or deactivated tenant")
    @PostMapping("/{id}/activate")
    public ResponseEntity<TenantResponse> activateTenant(
            @Parameter(description = "Tenant UUID") @PathVariable UUID id) {
        TenantResponse activated = tenantService.activateTenant(id);
        return ResponseEntity.ok(activated);
    }

    @Operation(summary = "Onboard tenant", description = "Atomically creates a new tenant and assigns the first admin user membership")
    @PostMapping("/onboard")
    public ResponseEntity<OnboardTenantResponse> onboardTenant(@Valid @RequestBody OnboardTenantRequest request) {
        OnboardTenantResponse response = tenantService.onboardTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
