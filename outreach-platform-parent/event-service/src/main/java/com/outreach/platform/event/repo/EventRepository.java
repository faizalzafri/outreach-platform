package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for outreach event management.
 */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    Page<EventEntity> findByStatus(EventStatus status, Pageable pageable);

    Optional<EventEntity> findByEventCode(String eventCode);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "LOWER(e.eventName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(e.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(e.eventCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<EventEntity> search(@Param("query") String query, Pageable pageable);
}
