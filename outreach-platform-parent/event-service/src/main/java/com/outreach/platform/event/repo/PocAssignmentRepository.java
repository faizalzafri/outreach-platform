package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.PocAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for POC user-to-event assignments.
 */
@Repository
public interface PocAssignmentRepository extends JpaRepository<PocAssignmentEntity, UUID> {

    List<PocAssignmentEntity> findByEventId(UUID eventId);

    List<PocAssignmentEntity> findByUserId(UUID userId);
}
