package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.EventEnrollmentEntity;
import com.outreach.platform.event.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for volunteer event enrollments.
 */
@Repository
public interface EventEnrollmentRepository extends JpaRepository<EventEnrollmentEntity, UUID> {

    List<EventEnrollmentEntity> findByEventId(UUID eventId);

    List<EventEnrollmentEntity> findByVolunteerId(UUID volunteerId);

    Optional<EventEnrollmentEntity> findByEventIdAndVolunteerId(UUID eventId, UUID volunteerId);

    boolean existsByEventIdAndVolunteerId(UUID eventId, UUID volunteerId);

    long countByEventIdAndAttendanceStatus(UUID eventId, AttendanceStatus attendanceStatus);
}
