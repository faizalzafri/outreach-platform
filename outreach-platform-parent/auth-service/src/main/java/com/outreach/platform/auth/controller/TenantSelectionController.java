package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.model.dto.SelectTenantRequest;
import com.outreach.platform.auth.model.dto.TenantMembershipResponse;
import com.outreach.platform.auth.service.TenantMembershipService;
import com.outreach.platform.auth.util.UserIdentifiers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Multi-tenant login support: lets a user who belongs to more than one tenant see their active
 * tenants and explicitly choose one, rather than silently operating under whichever tenant
 * {@link com.outreach.platform.auth.config.AuthorizationServerConfig}'s token customizer happened
 * to pick first. Selecting a tenant here doesn't itself return a new token — the caller (the SPA)
 * is expected to follow up with a normal refresh_token grant, which re-runs the same token
 * customizer and now finds the persisted selection.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
@Tag(name = "Tenant Selection", description = "Multi-tenant login: list and select active tenant memberships")
public class TenantSelectionController {

    private final TenantMembershipService tenantMembershipService;

    @Inject
    public TenantSelectionController(TenantMembershipService tenantMembershipService) {
        this.tenantMembershipService = tenantMembershipService;
    }

    @Operation(summary = "List active tenant memberships",
            description = "Returns the authenticated user's ACTIVE tenant memberships, for the tenant-selection page")
    @GetMapping("/tenant-memberships")
    public ResponseEntity<List<TenantMembershipResponse>> listTenantMemberships(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UserIdentifiers.fromUsername(jwt.getSubject());
        List<TenantMembershipResponse> memberships = tenantMembershipService.listActiveMembershipsForUser(userId)
                .stream()
                .map(summary -> new TenantMembershipResponse(
                        summary.tenantId(), summary.tenantName(), summary.tenantStatus(), summary.role()))
                .toList();
        return ResponseEntity.ok(memberships);
    }

    @Operation(summary = "Select active tenant",
            description = "Records the user's explicit choice of tenant; call the OAuth2 refresh_token grant afterward to obtain a token reflecting it")
    @PostMapping("/select-tenant")
    public ResponseEntity<Void> selectTenant(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody SelectTenantRequest request) {
        UUID userId = UserIdentifiers.fromUsername(jwt.getSubject());
        tenantMembershipService.selectTenant(userId, request.tenantId());
        return ResponseEntity.ok().build();
    }
}
