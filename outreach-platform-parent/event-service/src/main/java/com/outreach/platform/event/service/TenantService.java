package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.TenantEntity;
import com.outreach.platform.event.entity.TenantMembershipEntity;
import com.outreach.platform.event.model.TenantRole;
import com.outreach.platform.event.model.TenantStatus;
import com.outreach.platform.event.model.dto.CreateTenantRequest;
import com.outreach.platform.event.model.dto.OnboardTenantRequest;
import com.outreach.platform.event.model.dto.OnboardTenantResponse;
import com.outreach.platform.event.model.dto.TenantResponse;
import com.outreach.platform.event.model.dto.UpdateTenantRequest;
import com.outreach.platform.event.repo.TenantMembershipRepository;
import com.outreach.platform.event.repo.TenantRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for tenant CRUD operations, validation, and lifecycle status transitions.
 */
@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    @Inject
    public TenantService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    /**
     * Creates a new tenant with ACTIVE status.
     *
     * @param request the create request with name and slug
     * @return the created tenant response
     */
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsByName(request.name())) {
            throw new DuplicateTenantNameException(request.name());
        }
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new DuplicateTenantSlugException(request.slug());
        }

        TenantEntity entity = new TenantEntity();
        entity.setName(request.name());
        entity.setSlug(request.slug());
        entity.setStatus(TenantStatus.ACTIVE);

        TenantEntity saved = tenantRepository.save(entity);
        log.info("Created tenant '{}' with slug '{}' and id {}", saved.getName(), saved.getSlug(), saved.getId());
        return toResponse(saved);
    }

    /**
     * Returns a paginated list of all tenants.
     *
     * @param pageable pagination parameters
     * @return page of tenant responses
     */
    @Transactional(readOnly = true)
    public Page<TenantResponse> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Gets a tenant by ID.
     *
     * @param id the tenant UUID
     * @return the tenant response
     */
    @Transactional(readOnly = true)
    public TenantResponse getTenant(UUID id) {
        TenantEntity entity = findTenantOrThrow(id);
        return toResponse(entity);
    }

    /**
     * Updates a tenant's name.
     *
     * @param id      the tenant UUID
     * @param request the update request with new name
     * @return the updated tenant response
     */
    @Transactional
    public TenantResponse updateTenant(UUID id, UpdateTenantRequest request) {
        TenantEntity entity = findTenantOrThrow(id);

        if (!entity.getName().equals(request.name()) && tenantRepository.existsByName(request.name())) {
            throw new DuplicateTenantNameException(request.name());
        }

        entity.setName(request.name());
        TenantEntity saved = tenantRepository.save(entity);
        log.info("Updated tenant {} name to '{}'", id, saved.getName());
        return toResponse(saved);
    }

    /**
     * Deactivates a tenant.
     *
     * @param id the tenant UUID
     * @return the updated tenant response
     */
    @Transactional
    public TenantResponse deactivateTenant(UUID id) {
        TenantEntity entity = findTenantOrThrow(id);
        entity.setStatus(TenantStatus.DEACTIVATED);
        TenantEntity saved = tenantRepository.save(entity);
        log.info("Deactivated tenant {}", id);
        return toResponse(saved);
    }

    /**
     * Suspends a tenant.
     *
     * @param id the tenant UUID
     * @return the updated tenant response
     */
    @Transactional
    public TenantResponse suspendTenant(UUID id) {
        TenantEntity entity = findTenantOrThrow(id);
        entity.setStatus(TenantStatus.SUSPENDED);
        TenantEntity saved = tenantRepository.save(entity);
        log.info("Suspended tenant {}", id);
        return toResponse(saved);
    }

    /**
     * Reactivates a tenant.
     *
     * @param id the tenant UUID
     * @return the updated tenant response
     */
    @Transactional
    public TenantResponse activateTenant(UUID id) {
        TenantEntity entity = findTenantOrThrow(id);
        entity.setStatus(TenantStatus.ACTIVE);
        TenantEntity saved = tenantRepository.save(entity);
        log.info("Activated tenant {}", id);
        return toResponse(saved);
    }

    /**
     * Onboards a new tenant atomically: creates the tenant and assigns the first admin membership.
     *
     * @param request the onboarding request with tenant details and admin user ID
     * @return the onboarding response
     */
    @Transactional
    public OnboardTenantResponse onboardTenant(OnboardTenantRequest request) {
        // Validate tenant name uniqueness
        if (tenantRepository.existsByName(request.tenantName())) {
            throw new DuplicateTenantNameException(request.tenantName());
        }
        // Validate tenant slug uniqueness
        if (tenantRepository.existsBySlug(request.tenantSlug())) {
            throw new DuplicateTenantSlugException(request.tenantSlug());
        }
        // Validate admin user is not already registered in any tenant
        if (membershipRepository.existsByUserId(request.adminUserId())) {
            throw new AdminUserAlreadyAssignedException(request.adminUserId());
        }

        // Create the tenant
        TenantEntity tenant = new TenantEntity();
        tenant.setName(request.tenantName());
        tenant.setSlug(request.tenantSlug());
        tenant.setStatus(TenantStatus.ACTIVE);
        TenantEntity savedTenant = tenantRepository.save(tenant);

        // Create the admin membership
        TenantMembershipEntity membership = new TenantMembershipEntity();
        membership.setTenantId(savedTenant.getId());
        membership.setUserId(request.adminUserId());
        membership.setRole(TenantRole.ADMIN);
        membershipRepository.save(membership);

        log.info("Onboarded tenant '{}' (slug='{}', id={}) with admin user {}",
                savedTenant.getName(), savedTenant.getSlug(), savedTenant.getId(), request.adminUserId());

        return new OnboardTenantResponse(
                savedTenant.getId(),
                savedTenant.getName(),
                savedTenant.getSlug(),
                request.adminUserId()
        );
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private TenantEntity findTenantOrThrow(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    private TenantResponse toResponse(TenantEntity entity) {
        return new TenantResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getStatus(),
                entity.getCreatedDate()
        );
    }

    // ─── Exceptions ─────────────────────────────────────────────────────────────

    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(UUID id) {
            super("Tenant with ID " + id + " not found");
        }
    }

    public static class DuplicateTenantNameException extends RuntimeException {
        public DuplicateTenantNameException(String name) {
            super("Tenant with name '" + name + "' already exists");
        }
    }

    public static class DuplicateTenantSlugException extends RuntimeException {
        public DuplicateTenantSlugException(String slug) {
            super("Tenant with slug '" + slug + "' already exists");
        }
    }

    public static class AdminUserAlreadyAssignedException extends RuntimeException {
        public AdminUserAlreadyAssignedException(UUID userId) {
            super("Admin user " + userId + " is already registered in another tenant");
        }
    }
}
