package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.BeneficiaryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for beneficiary organizations.
 */
@Repository
public interface BeneficiaryRepository extends JpaRepository<BeneficiaryEntity, UUID> {

    List<BeneficiaryEntity> findByCity(String city);

    Page<BeneficiaryEntity> findByActive(boolean active, Pageable pageable);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.organization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.city) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<BeneficiaryEntity> search(@Param("query") String query, Pageable pageable);
}
