package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.Tenant;
import com.outreach.platform.auth.entity.TenantMembership;
import com.outreach.platform.auth.model.TenantStatus;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.auth.repo.TenantRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resolves tenant memberships for authenticated users at token-issuance time. */
@Named
public class TenantMembershipService {

    private final TenantMembershipRepository tenantMembershipRepository;
    private final TenantRepository tenantRepository;

    @Inject
    public TenantMembershipService(TenantMembershipRepository tenantMembershipRepository,
                                   TenantRepository tenantRepository) {
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.tenantRepository = tenantRepository;
    }


    public List<TenantMembership> findMembershipsByUserId(UUID userId) {
        return tenantMembershipRepository.findByUserId(userId);
    }

    /**
     * Returns the user's active tenant: the ACTIVE-tenant membership they most recently and
     * explicitly selected (via the multi-tenant login flow), or — if they've never made an
     * explicit selection — the first ACTIVE membership found, matching the original behavior for
     * users who only ever belong to one tenant.
     */
    public Optional<Tenant> getActiveTenantForUser(UUID userId) {
        List<TenantMembership> memberships = tenantMembershipRepository.findByUserId(userId);

        Optional<TenantMembership> mostRecentlySelected = memberships.stream()
                .filter(m -> m.getLastSelectedAt() != null)
                .filter(m -> isActive(m.getTenantId()))
                .max(Comparator.comparing(TenantMembership::getLastSelectedAt));

        if (mostRecentlySelected.isPresent()) {
            return tenantRepository.findById(mostRecentlySelected.get().getTenantId());
        }

        for (TenantMembership membership : memberships) {
            Optional<Tenant> tenant = tenantRepository.findById(membership.getTenantId());
            if (tenant.isPresent() && tenant.get().getStatus() == TenantStatus.ACTIVE) {
                return tenant;
            }
        }

        return Optional.empty();
    }

    /**
     * True when the user belongs to more than one ACTIVE tenant and has never explicitly chosen
     * one — the signal the frontend uses to redirect to the tenant-selection page instead of
     * trusting the (arbitrary) tenant the freshly-issued token currently carries.
     */
    public boolean isTenantSelectionRequired(UUID userId) {
        List<TenantMembership> memberships = tenantMembershipRepository.findByUserId(userId);

        boolean anySelected = memberships.stream().anyMatch(m -> m.getLastSelectedAt() != null);
        if (anySelected) {
            return false;
        }

        long activeCount = memberships.stream()
                .filter(m -> isActive(m.getTenantId()))
                .count();
        return activeCount > 1;
    }

    /**
     * Lists the user's ACTIVE tenant memberships with tenant details, for the tenant-selection
     * page.
     */
    public List<TenantSummary> listActiveMembershipsForUser(UUID userId) {
        return tenantMembershipRepository.findByUserId(userId).stream()
                .map(membership -> tenantRepository.findById(membership.getTenantId())
                        .filter(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                        .map(tenant -> new TenantSummary(tenant.getId(), tenant.getName(), tenant.getStatus(), membership.getRole().name())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Records the user's explicit choice of tenant. The next token issuance (initial login or
     * refresh) will resolve to this tenant via {@link #getActiveTenantForUser}.
     *
     * @throws MembershipNotFoundException if the user has no membership in the given tenant
     * @throws TenantNotActiveException    if the tenant is not ACTIVE
     */
    public void selectTenant(UUID userId, UUID tenantId) {
        TenantMembership membership = tenantMembershipRepository.findByUserId(userId).stream()
                .filter(m -> m.getTenantId().equals(tenantId))
                .findFirst()
                .orElseThrow(() -> new MembershipNotFoundException(userId, tenantId));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new MembershipNotFoundException(userId, tenantId));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new TenantNotActiveException(tenantId);
        }

        membership.setLastSelectedAt(Instant.now());
        tenantMembershipRepository.save(membership);
    }

    private boolean isActive(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
                .orElse(false);
    }

    /** Lightweight tenant summary for the tenant-selection page — id, name, status, and the user's role in it. */
    public record TenantSummary(UUID tenantId, String tenantName, TenantStatus tenantStatus, String role) {
    }

    public static class MembershipNotFoundException extends RuntimeException {
        public MembershipNotFoundException(UUID userId, UUID tenantId) {
            super("User " + userId + " has no membership in tenant " + tenantId);
        }
    }

    public static class TenantNotActiveException extends RuntimeException {
        public TenantNotActiveException(UUID tenantId) {
            super("Tenant " + tenantId + " is not active");
        }
    }
}
