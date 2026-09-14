package com.outreach.platform.ai.repo;

import com.outreach.platform.ai.entity.AiJobDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data MongoDB repository for AI job documents.
 */
public interface AiJobRepository extends MongoRepository<AiJobDocument, String> {

    /**
     * Tenant-scoped lookup — {@code AiJobDocument} has no Hibernate {@code @Filter} equivalent
     * (it's a Mongo document, not a JPA entity), so unlike the JPA services there was never any
     * tenant scoping attempted here at all until this method existed. Callers must use this
     * instead of bare {@code findById}, falling back to the unscoped method only when
     * {@code TenantContext} is empty (the {@code PLATFORM_ADMIN} case) — see
     * docs/specs/platform-hardening/requirements.md Finding 0 / Task 0.5.7.
     */
    Optional<AiJobDocument> findByIdAndTenantId(String id, UUID tenantId);
}
