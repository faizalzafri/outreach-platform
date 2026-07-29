package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TenantEntity}.
 */
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    /**
     * Find a tenant by its unique name.
     *
     * @param name the tenant name
     * @return the tenant if found
     */
    Optional<TenantEntity> findByName(String name);

    /**
     * Find a tenant by its unique slug.
     *
     * @param slug the URL-friendly tenant identifier
     * @return the tenant if found
     */
    Optional<TenantEntity> findBySlug(String slug);

    /**
     * Check if a tenant with the given name exists.
     *
     * @param name the tenant name
     * @return true if a tenant with this name exists
     */
    boolean existsByName(String name);

    /**
     * Check if a tenant with the given slug exists.
     *
     * @param slug the tenant slug
     * @return true if a tenant with this slug exists
     */
    boolean existsBySlug(String slug);
}
