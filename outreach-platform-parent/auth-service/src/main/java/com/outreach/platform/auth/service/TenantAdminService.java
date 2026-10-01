package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.Tenant;
import com.outreach.platform.auth.entity.TenantMembership;
import com.outreach.platform.auth.model.TenantRole;
import com.outreach.platform.auth.model.TenantStatus;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.auth.repo.TenantRepository;
import com.outreach.platform.auth.service.AccountAdminService.InviteCommand;
import com.outreach.platform.auth.service.AccountAdminService.UserSummary;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Platform-admin management of organizations (tenants): create one together with its first
 * admin's invitation, rename it, and move it between active, suspended and deactivated. Users of
 * a tenant that is not active cannot get new tokens, and the gateway refuses its requests.
 */
@Named
public class TenantAdminService {

    private static final Logger log = LoggerFactory.getLogger(TenantAdminService.class);

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final AccountAdminService accounts;

    @Inject
    public TenantAdminService(TenantRepository tenants, TenantMembershipRepository memberships,
                              AccountAdminService accounts) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.accounts = accounts;
    }

    public record TenantSummary(UUID id, String name, String slug, TenantStatus status, Instant createdDate,
                                long memberCount) {
    }

    public record TenantDetail(UUID id, String name, String slug, TenantStatus status, Instant createdDate,
                               Instant lastModifiedDate, String lastModifiedBy, Map<TenantRole, Long> membersByRole) {
    }

    public record CreatedTenant(TenantSummary tenant, UserSummary admin) {
    }

    @Transactional(readOnly = true)
    public Page<TenantSummary> list(String search, Pageable pageable) {
        Page<Tenant> page = search == null || search.isBlank()
                ? tenants.findAll(pageable)
                : tenants.findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(search.trim(), search.trim(), pageable);
        Map<UUID, Long> counts = page.getContent().isEmpty() ? Map.of() : memberCounts();
        return page.map(t -> summary(t, counts.getOrDefault(t.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public TenantDetail get(UUID id) {
        Tenant tenant = tenantOrThrow(id);
        Map<TenantRole, Long> byRole = memberships.findByTenantId(id).stream()
                .collect(Collectors.groupingBy(TenantMembership::getRole, Collectors.counting()));
        return new TenantDetail(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getStatus(),
                tenant.getCreatedDate(), tenant.getLastModifiedDate(), tenant.getLastModifiedBy(), byRole);
    }

    @Transactional(readOnly = true)
    public TenantStatus status(UUID id) {
        return tenants.findById(id).map(Tenant::getStatus).orElse(TenantStatus.DEACTIVATED);
    }

    /**
     * Creates an active organization and invites its first admin by email, in one transaction:
     * either both happen or neither does.
     */
    @Transactional
    public CreatedTenant create(String name, String slug, InviteCommand firstAdmin, String createdBy) {
        if (tenants.existsByNameIgnoreCase(name.trim())) {
            throw new AccountAdminService.AccountConflictException("An organization named '" + name.trim() + "' already exists");
        }
        if (tenants.existsBySlug(slug)) {
            throw new AccountAdminService.AccountConflictException("The short name '" + slug + "' is already taken");
        }
        Tenant tenant = new Tenant();
        tenant.setName(name.trim());
        tenant.setSlug(slug);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenants.save(tenant);

        UserSummary admin = accounts.invite(tenant.getId(),
                new InviteCommand(firstAdmin.username(), firstAdmin.email(), firstAdmin.displayName(), TenantRole.ADMIN),
                createdBy);
        log.info("Created organization '{}' ({}) and invited its first admin userId={}", tenant.getName(),
                tenant.getId(), admin.id());
        return new CreatedTenant(summary(tenant, 1), admin);
    }

    @Transactional
    public TenantDetail rename(UUID id, String name) {
        Tenant tenant = tenantOrThrow(id);
        if (!tenant.getName().equalsIgnoreCase(name.trim()) && tenants.existsByNameIgnoreCase(name.trim())) {
            throw new AccountAdminService.AccountConflictException("An organization named '" + name.trim() + "' already exists");
        }
        tenant.setName(name.trim());
        return get(id);
    }

    @Transactional
    public TenantDetail changeStatus(UUID id, TenantStatus status) {
        Tenant tenant = tenantOrThrow(id);
        TenantStatus previous = tenant.getStatus();
        tenant.setStatus(status);
        log.info("Organization {} status changed: {} -> {}", id, previous, status);
        return get(id);
    }

    private Map<UUID, Long> memberCounts() {
        return memberships.findAll().stream()
                .collect(Collectors.groupingBy(TenantMembership::getTenantId, Collectors.counting()));
    }

    private Tenant tenantOrThrow(UUID id) {
        return tenants.findById(id).orElseThrow(() -> new TenantNotFoundException(id));
    }

    private static TenantSummary summary(Tenant tenant, long members) {
        return new TenantSummary(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getStatus(),
                tenant.getCreatedDate(), members);
    }

    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(UUID id) {
            super("Organization " + id + " not found");
        }
    }
}
