package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.EventBeneficiaryEntity;
import com.outreach.platform.event.entity.EventBeneficiaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for event-beneficiary associations.
 */
@Repository
public interface EventBeneficiaryRepository extends JpaRepository<EventBeneficiaryEntity, EventBeneficiaryId> {

    List<EventBeneficiaryEntity> findByIdBeneficiaryId(UUID beneficiaryId);

    List<EventBeneficiaryEntity> findByIdEventId(UUID eventId);
}
