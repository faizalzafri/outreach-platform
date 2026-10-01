package com.outreach.platform.auth.entity;

import com.outreach.platform.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A tenant's own password and OTP settings. Null password fields and missing OTP purposes fall
 * back to the platform defaults ({@code auth-server.security.*}).
 */
@Entity
@Table(name = "tenant_security_policy")
@Getter
@Setter
public class TenantSecurityPolicy extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "password_min_length")
    private Integer passwordMinLength;

    @Column(name = "password_history_count")
    private Integer passwordHistoryCount;

    /** Purpose name → settings ({@code enabled}, {@code length}, {@code ttlSeconds}, {@code maxAttempts}, {@code resendCooldownSeconds}). */
    @Type(JsonType.class)
    @Column(name = "otp", nullable = false, columnDefinition = "jsonb")
    private Map<String, Map<String, Object>> otp = new HashMap<>();
}
