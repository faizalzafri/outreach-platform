package com.outreach.platform.event.controller;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.model.dto.AddMemberRequest;
import com.outreach.platform.event.model.dto.MemberResponse;
import com.outreach.platform.event.service.TenantMembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for managing tenant memberships with cross-tenant access control.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/tenants/{tenantId}/members")
@Tag(name = "Tenant Membership", description = "Manage tenant members — add, remove, and list")
public class TenantMembershipController {

    private final TenantMembershipService membershipService;

    @Inject
    public TenantMembershipController(TenantMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Operation(summary = "List tenant members", description = "Returns paginated members of the specified tenant")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TENANT_ADMIN', 'ROLE_PLATFORM_ADMIN')")
    public ResponseEntity<Page<MemberResponse>> listMembers(
            @Parameter(description = "Tenant UUID") @PathVariable UUID tenantId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        enforceOwnTenantAccess(tenantId, authentication);
        Page<MemberResponse> members = membershipService.listMembers(tenantId, pageable);
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Add tenant member", description = "Adds a user to the tenant with the specified role")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_TENANT_ADMIN', 'ROLE_PLATFORM_ADMIN')")
    public ResponseEntity<MemberResponse> addMember(
            @Parameter(description = "Tenant UUID") @PathVariable UUID tenantId,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {

        enforceOwnTenantAccess(tenantId, authentication);
        MemberResponse response = membershipService.addMember(tenantId, request.userId(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Remove tenant member", description = "Removes a user from the tenant")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_TENANT_ADMIN', 'ROLE_PLATFORM_ADMIN')")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Tenant UUID") @PathVariable UUID tenantId,
            @Parameter(description = "User UUID to remove") @PathVariable UUID userId,
            Authentication authentication) {

        enforceOwnTenantAccess(tenantId, authentication);
        membershipService.removeMember(tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /** Ensures TENANT_ADMINs can only manage their own tenant; PLATFORM_ADMINs bypass this. */
    private void enforceOwnTenantAccess(UUID tenantId, Authentication authentication) {
        boolean isPlatformAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PLATFORM_ADMIN"::equals);

        if (isPlatformAdmin) {
            return;
        }

        UUID currentTenantId = TenantContext.getCurrentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
            throw new AccessDeniedException("Operation not permitted on tenant " + tenantId);
        }
    }
}
