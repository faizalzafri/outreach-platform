package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.OtpChallenge;
import com.outreach.platform.auth.model.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findFirstByUserIdAndPurposeOrderByCreatedDateDesc(UUID userId, OtpPurpose purpose);

    List<OtpChallenge> findByUserIdAndPurposeAndConsumedAtIsNull(UUID userId, OtpPurpose purpose);
}
