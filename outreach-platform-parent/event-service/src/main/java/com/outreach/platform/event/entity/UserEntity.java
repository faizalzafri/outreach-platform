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

/**
 * A user as this service knows them: a read-only directory entry for POC assignment, teams and
 * reports. auth-service owns accounts and credentials and keeps these rows in sync through
 * {@code identity.user-changed} events (see {@code UserDirectorySync}); nothing here edits them.
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

    @Column(name = "display_name", length = 100)
    private String displayName;

    @PiiField(description = "User email address")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "email_encrypted")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public UserEntity() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
