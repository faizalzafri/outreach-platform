package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.VolunteerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for volunteer profiles.
 */
@Repository
public interface VolunteerRepository extends JpaRepository<VolunteerEntity, UUID> {

    Optional<VolunteerEntity> findByEmployeeId(String employeeId);

    Page<VolunteerEntity> findByBaseLocation(String baseLocation, Pageable pageable);

    @Query("SELECT v FROM VolunteerEntity v WHERE " +
            "LOWER(v.skills) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(v.baseLocation) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(v.department) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<VolunteerEntity> searchBySkillsOrLocation(@Param("query") String query, Pageable pageable);

    @Query("SELECT v FROM VolunteerEntity v ORDER BY v.totalEventsParticipated DESC NULLS LAST")
    Page<VolunteerEntity> findTopVolunteers(Pageable pageable);
}
