package com.outreach.platform.event.repo;

import com.outreach.platform.event.model.PlatformAdminAuditDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data MongoDB repository for Platform_Admin cross-tenant audit log entries.
 */
@Repository
public interface PlatformAdminAuditRepository extends MongoRepository<PlatformAdminAuditDocument, String> {

    /**
     * Finds audit entries by admin user ID, ordered by timestamp descending.
     */
    List<PlatformAdminAuditDocument> findByAdminUserIdOrderByTimestampDesc(String adminUserId);

    /**
     * Finds audit entries targeting a specific tenant, ordered by timestamp descending.
     */
    List<PlatformAdminAuditDocument> findByTargetTenantIdOrderByTimestampDesc(String targetTenantId);

    /**
     * Finds audit entries by admin user ID with pagination.
     */
    Page<PlatformAdminAuditDocument> findByAdminUserId(String adminUserId, Pageable pageable);

    /**
     * Finds audit entries within a time range.
     */
    List<PlatformAdminAuditDocument> findByTimestampBetween(Instant from, Instant to);
}
