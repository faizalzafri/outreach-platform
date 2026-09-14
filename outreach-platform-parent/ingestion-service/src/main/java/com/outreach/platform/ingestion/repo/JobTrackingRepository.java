package com.outreach.platform.ingestion.repo;

import com.outreach.platform.ingestion.model.JobTrackingDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data MongoDB repository for job tracking documents.
 */
public interface JobTrackingRepository extends MongoRepository<JobTrackingDocument, String> {

    Page<JobTrackingDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Tenant-scoped equivalents of the two methods above — {@code JobTrackingDocument} had no
     * tenant scoping attempted at all until this (no Hibernate {@code @Filter} equivalent exists
     * for Mongo), so {@code findAllByOrderByCreatedAtDesc} and bare {@code findById} were both
     * genuine, exploitable cross-tenant reads: docs/specs/platform-hardening/requirements.md
     * Finding 0 / Task 0.5.7. Callers must use these, falling back to the unscoped method only
     * when {@code TenantContext} is empty (the {@code PLATFORM_ADMIN} case).
     */
    Optional<JobTrackingDocument> findByIdAndTenantId(String id, UUID tenantId);

    Page<JobTrackingDocument> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
