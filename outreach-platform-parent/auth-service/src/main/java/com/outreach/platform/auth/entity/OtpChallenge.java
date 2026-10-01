package com.outreach.platform.auth.entity;

import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** One issued one-time passcode. Only a salted hash of the code is stored. */
@Entity
@Table(name = "otp_challenges")
@Getter
@Setter
public class OtpChallenge extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public boolean isOpen(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now) && attempts < maxAttempts;
    }
}
