package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for outreach event management.
 */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    /**
     * Tenant-scoped primary-key lookup. {@link #findById(Object)} (inherited from
     * {@code JpaRepository}) compiles to {@code EntityManager.find()}, which Hibernate's
     * {@code @Filter} mechanism does NOT apply to — a session-level filter only affects
     * HQL/JPQL/Criteria queries, not direct identifier loads. Derived query methods like this one
     * <em>do</em> compile to JPQL and are correctly filtered, so this explicit tenant condition —
     * not the inherited filter — is what actually enforces isolation here. Use this instead of
     * {@code findById} for every tenant-scoped lookup; see
     * {@code docs/specs/platform-hardening/requirements.md} Finding 0 / Requirement 0 for the
     * regression test that proved {@code findById} alone lets a cross-tenant read through.
     */
    Optional<EventEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<EventEntity> findByStatus(EventStatus status, Pageable pageable);

    Optional<EventEntity> findByEventCode(String eventCode);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "LOWER(e.eventName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(e.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(e.eventCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<EventEntity> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:city IS NULL OR LOWER(e.city) = LOWER(CAST(:city AS string))) AND " +
            "(:category IS NULL OR LOWER(e.category) = LOWER(CAST(:category AS string))) AND " +
            "(:dateFrom IS NULL OR e.eventDate >= :dateFrom) AND " +
            "(:dateTo IS NULL OR e.eventDate <= :dateTo)")
    Page<EventEntity> findByFilters(
            @Param("status") EventStatus status,
            @Param("city") String city,
            @Param("category") String category,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable);

    @Query("SELECT e FROM EventEntity e WHERE " +
            "e.eventDate >= :rangeStart AND e.eventDate <= :rangeEnd " +
            "ORDER BY e.eventDate ASC")
    Page<EventEntity> findByDateRange(
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd,
            Pageable pageable);

    long countByStatus(EventStatus status);

    long countByStatusIn(Collection<EventStatus> statuses);
}
