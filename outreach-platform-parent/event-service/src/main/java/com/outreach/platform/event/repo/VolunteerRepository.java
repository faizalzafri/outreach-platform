package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.VolunteerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
