package com.outreach.platform.event.repo;

import com.outreach.platform.event.model.ActivityEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data MongoDB repository for {@link ActivityEvent} documents.
 */
@Repository
public interface ActivityEventRepository extends MongoRepository<ActivityEvent, String> {

    /**
     * Find activity events for a tenant ordered by timestamp descending.
     */
    List<ActivityEvent> findByTenantIdOrderByTimestampDesc(UUID tenantId, Pageable pageable);

    /**
     * Find activity events for a tenant before a given cursor timestamp, ordered by timestamp descending.
     */
    List<ActivityEvent> findByTenantIdAndTimestampBeforeOrderByTimestampDesc(UUID tenantId, Instant cursor, Pageable pageable);

    /**
     * Find activity events for a tenant and team ordered by timestamp descending.
     */
    List<ActivityEvent> findByTenantIdAndTeamIdOrderByTimestampDesc(UUID tenantId, UUID teamId, Pageable pageable);

    /**
     * Find activity events for a tenant and team before a given cursor timestamp.
     */
    List<ActivityEvent> findByTenantIdAndTeamIdAndTimestampBeforeOrderByTimestampDesc(UUID tenantId, UUID teamId, Instant cursor, Pageable pageable);
}
