package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.VolunteerFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for volunteer feedback submissions.
 */
@Repository
public interface VolunteerFeedbackRepository extends JpaRepository<VolunteerFeedbackEntity, UUID> {

    List<VolunteerFeedbackEntity> findByEventId(UUID eventId);

    List<VolunteerFeedbackEntity> findByVolunteerId(UUID volunteerId);
}
