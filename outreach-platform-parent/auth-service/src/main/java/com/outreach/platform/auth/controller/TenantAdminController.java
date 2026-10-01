package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.model.TenantStatus;
import com.outreach.platform.auth.service.AccountAdminService.InviteCommand;
import com.outreach.platform.auth.service.TenantAdminService;
import com.outreach.platform.auth.service.TenantAdminService.CreatedTenant;
import com.outreach.platform.auth.service.TenantAdminService.TenantDetail;
import com.outreach.platform.auth.service.TenantAdminService.TenantSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Organizations (tenants), managed by platform admins. {@code /current} is open to every
 * signed-in user and returns their own organization.
 */
@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
@Tag(name = "Organizations", description = "Create organizations and manage their lifecycle")
public class TenantAdminController {

    private final TenantAdminService tenants;

    @Inject
    public TenantAdminController(TenantAdminService tenants) {
        this.tenants = tenants;
    }

    public record CreateTenantRequest(
            @NotBlank @Size(min = 3, max = 100) String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$",
                    message = "3–50 lower-case letters, digits or hyphens, not starting or ending with a hyphen") String slug,
            @NotBlank @Size(max = 100) String adminDisplayName,
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
                    message = "3–50 letters, digits, dots, hyphens or underscores") String adminUsername,
            @NotBlank @Email @Size(max = 254) String adminEmail) {
    }

    public record RenameTenantRequest(@NotBlank @Size(min = 3, max = 100) String name) {
    }

    @Operation(summary = "List organizations", description = "Searchable by name or short name")
    @GetMapping
    public Page<TenantSummary> list(@RequestParam(required = false) String search,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return tenants.list(search, PageRequest.of(page, Math.min(size, 100), Sort.by("name")));
    }

    @Operation(summary = "Your own organization")
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public TenantDetail current(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = Caller.of(jwt).tenantId();
        if (tenantId == null) {
            throw new TenantAdminService.TenantNotFoundException(null);
        }
        return tenants.get(tenantId);
    }

    @Operation(summary = "One organization, with member counts per role")
    @GetMapping("/{id}")
    public TenantDetail get(@PathVariable UUID id) {
        return tenants.get(id);
    }

    @Operation(summary = "Create an organization",
            description = "Creates it active and emails its first admin an activation link")
    @PostMapping
    public ResponseEntity<CreatedTenant> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CreateTenantRequest request) {
        CreatedTenant created = tenants.create(request.name(), request.slug(),
                new InviteCommand(request.adminUsername(), request.adminEmail(), request.adminDisplayName(), null),
                jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Rename an organization")
    @PutMapping("/{id}")
    public TenantDetail rename(@PathVariable UUID id, @Valid @RequestBody RenameTenantRequest request) {
        return tenants.rename(id, request.name());
    }

    @Operation(summary = "Suspend", description = "Members cannot sign in or use the platform until reactivated")
    @PostMapping("/{id}/suspend")
    public TenantDetail suspend(@PathVariable UUID id) {
        return tenants.changeStatus(id, TenantStatus.SUSPENDED);
    }

    @Operation(summary = "Reactivate")
    @PostMapping("/{id}/activate")
    public TenantDetail activate(@PathVariable UUID id) {
        return tenants.changeStatus(id, TenantStatus.ACTIVE);
    }

    @Operation(summary = "Deactivate", description = "For organizations that have left; data is kept")
    @PostMapping("/{id}/deactivate")
    public TenantDetail deactivate(@PathVariable UUID id) {
        return tenants.changeStatus(id, TenantStatus.DEACTIVATED);
    }
}
