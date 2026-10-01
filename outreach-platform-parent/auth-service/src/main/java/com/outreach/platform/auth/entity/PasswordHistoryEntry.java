package com.outreach.platform.auth.entity;

import com.outreach.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A previously used password hash, checked so users cannot cycle back to recent passwords. */
@Entity
@Table(name = "password_history")
@Getter
@Setter
@NoArgsConstructor
public class PasswordHistoryEntry extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 500)
    private String passwordHash;

    public PasswordHistoryEntry(UUID userId, String passwordHash) {
        this.userId = userId;
        this.passwordHash = passwordHash;
    }
}
