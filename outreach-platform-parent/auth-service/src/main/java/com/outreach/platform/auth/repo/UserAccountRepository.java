package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmailHash(String emailHash);

    boolean existsByUsername(String username);

    boolean existsByEmailHash(String emailHash);

    boolean existsByPlatformAdminTrue();

    List<UserAccount> findByIdIn(Collection<UUID> ids);
}
