package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for platform user accounts.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByEnabled(boolean enabled);

    long countByAccountLocked(boolean accountLocked);

    Page<UserEntity> findByRole(UserRole role, Pageable pageable);
}
