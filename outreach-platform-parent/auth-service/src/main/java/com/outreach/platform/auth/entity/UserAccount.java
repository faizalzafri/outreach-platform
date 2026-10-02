package com.outreach.platform.auth.entity;

import com.outreach.platform.auth.model.AccountStatus;
import com.outreach.platform.common.entity.BaseEntity;
import com.outreach.platform.common.pii.AesEncryptionConverter;
import com.outreach.platform.common.pii.PiiBlindIndex;
import com.outreach.platform.common.pii.PiiField;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A sign-in account: the single source of truth for who can authenticate. Tenant roles live in
 * {@link TenantMembership}; the only global role is the platform-admin flag.
 */
@Entity
@Table(name = "auth_users")
@Getter
@Setter
public class UserAccount extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Encoded password ({@code {bcrypt}...}); null until an invited account is activated. */
    @Column(name = "password", length = 500)
    private String password;

    /** Mirrors {@link #status} for Spring Security's column; kept in sync by {@link #setStatus}. */
    @Column(name = "enabled", nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean enabled;

    @PiiField(description = "Account email address")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "email_encrypted", length = 512)
    @Setter(AccessLevel.NONE)
    private String email;

    /** Keyed hash of the email for lookups (see {@link PiiBlindIndex}); maintained by {@link #setEmail}. */
    @Column(name = "email_hash", length = 64)
    @Setter(AccessLevel.NONE)
    private String emailHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @PiiField(description = "Account phone number")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "phone_encrypted", length = 512)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Setter(AccessLevel.NONE)
    private AccountStatus status = AccountStatus.INVITED;

    @Column(name = "platform_admin", nullable = false)
    private boolean platformAdmin;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
        this.emailHash = PiiBlindIndex.of(email);
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
        this.enabled = status == AccountStatus.ACTIVE;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
