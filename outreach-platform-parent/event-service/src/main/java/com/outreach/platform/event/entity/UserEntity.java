package com.outreach.platform.event.entity;

import com.outreach.platform.common.pii.AesEncryptionConverter;
import com.outreach.platform.common.pii.PiiField;
import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.event.model.UserRole;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Platform user account with role-based access.
 *
 * <p>See {@link EventEntity} for why the audit columns are overridden rather than renamed.
 * Unlike most other entities in this service, {@code created_by} already matched
 * {@code BaseEntity}'s default column name, so only {@code createdDate}/{@code lastModifiedDate}/
 * {@code lastModifiedBy} need overriding.
 */
@Entity
@Table(name = "users")
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class UserEntity extends TenantAwareBaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @PiiField(description = "User email address")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "email_encrypted")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public UserEntity() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    // id, tenantId, createdDate/lastModifiedDate/createdBy/lastModifiedBy, and version
    // are inherited — see the class-level @AttributeOverrides.
}
