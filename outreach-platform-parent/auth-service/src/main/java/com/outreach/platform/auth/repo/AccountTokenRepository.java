package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.AccountToken;
import com.outreach.platform.auth.model.AccountTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    Optional<AccountToken> findByTokenHashAndPurpose(String tokenHash, AccountTokenPurpose purpose);

    List<AccountToken> findByUserIdAndPurposeAndUsedAtIsNull(UUID userId, AccountTokenPurpose purpose);
}
