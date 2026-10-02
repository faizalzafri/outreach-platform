package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.AuthServiceProperties;
import com.outreach.platform.auth.config.AuthServiceProperties.SecurityProperties.OtpSettings;
import com.outreach.platform.auth.entity.TenantSecurityPolicy;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.auth.repo.TenantSecurityPolicyRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the password and OTP policy that applies to an account. Platform defaults come from
 * configuration; each tenant can override them. A user in several tenants gets the strictest
 * combination, so belonging to a relaxed tenant never weakens a stricter one.
 */
@Named
public class SecurityPolicyService {

    private final AuthServiceProperties.SecurityProperties defaults;
    private final TenantSecurityPolicyRepository policies;
    private final TenantMembershipRepository memberships;

    @Inject
    public SecurityPolicyService(AuthServiceProperties properties,
                                 TenantSecurityPolicyRepository policies,
                                 TenantMembershipRepository memberships) {
        this.defaults = properties.security();
        this.policies = policies;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public int passwordMinLength(UserAccount account) {
        return tenantPolicies(account).stream()
                .map(TenantSecurityPolicy::getPasswordMinLength)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .map(min -> Math.max(min, defaults.passwordPolicy().minLength()))
                .orElse(defaults.passwordPolicy().minLength());
    }

    @Transactional(readOnly = true)
    public int passwordHistoryCount(UserAccount account) {
        return tenantPolicies(account).stream()
                .map(TenantSecurityPolicy::getPasswordHistoryCount)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .map(count -> Math.max(count, defaults.passwordPolicy().historyCount()))
                .orElse(defaults.passwordPolicy().historyCount());
    }

    /** The OTP settings for this account and purpose; disabled unless the platform or a tenant enables it. */
    @Transactional(readOnly = true)
    public OtpSettings otpSettings(UserAccount account, OtpPurpose purpose) {
        OtpSettings platform = platformOtp(purpose);
        List<OtpSettings> tenantSettings = tenantPolicies(account).stream()
                .map(policy -> fromJson(policy.getOtp().get(purpose.name()), platform))
                .toList();
        if (tenantSettings.isEmpty()) {
            return platform;
        }
        // Strictest wins: enabled anywhere → enabled, using the tightest of the enabling settings.
        return tenantSettings.stream()
                .filter(OtpSettings::enabled)
                .min(Comparator.comparing(OtpSettings::ttl).thenComparing(OtpSettings::maxAttempts))
                .orElse(platform.enabled() ? platform : tenantSettings.getFirst());
    }

    public OtpSettings platformOtp(OtpPurpose purpose) {
        return defaults.otp().purposes().getOrDefault(purpose.name(), OtpSettings.DISABLED);
    }

    /** One tenant's effective settings for its admin screen (platform defaults where not overridden). */
    @Transactional(readOnly = true)
    public TenantView tenantView(UUID tenantId) {
        Optional<TenantSecurityPolicy> policy = policies.findByTenantId(tenantId);
        Map<OtpPurpose, OtpSettings> otp = new java.util.EnumMap<>(OtpPurpose.class);
        for (OtpPurpose purpose : OtpPurpose.values()) {
            OtpSettings platform = platformOtp(purpose);
            otp.put(purpose, policy.map(p -> fromJson(p.getOtp().get(purpose.name()), platform)).orElse(platform));
        }
        return new TenantView(
                policy.map(TenantSecurityPolicy::getPasswordMinLength).orElse(defaults.passwordPolicy().minLength()),
                policy.map(TenantSecurityPolicy::getPasswordHistoryCount).orElse(defaults.passwordPolicy().historyCount()),
                defaults.passwordPolicy().minLength(),
                defaults.passwordPolicy().historyCount(),
                otp);
    }

    /**
     * Saves a tenant's policy. The password minimum and history may only be at least as strict as
     * the platform's; OTP settings are the tenant's to choose within the API's bounds.
     */
    @Transactional
    public TenantView update(UUID tenantId, int passwordMinLength, int passwordHistoryCount,
                             Map<OtpPurpose, OtpSettings> otp) {
        if (passwordMinLength < defaults.passwordPolicy().minLength()) {
            throw new IllegalArgumentException("Minimum password length cannot be below the platform minimum of "
                    + defaults.passwordPolicy().minLength());
        }
        if (passwordHistoryCount < defaults.passwordPolicy().historyCount()) {
            throw new IllegalArgumentException("Password history cannot be below the platform minimum of "
                    + defaults.passwordPolicy().historyCount());
        }
        TenantSecurityPolicy policy = policies.findByTenantId(tenantId).orElseGet(() -> {
            TenantSecurityPolicy created = new TenantSecurityPolicy();
            created.setTenantId(tenantId);
            return created;
        });
        policy.setPasswordMinLength(passwordMinLength);
        policy.setPasswordHistoryCount(passwordHistoryCount);
        Map<String, Map<String, Object>> json = new java.util.HashMap<>();
        otp.forEach((purpose, settings) -> json.put(purpose.name(), toJson(settings)));
        policy.setOtp(json);
        policies.save(policy);
        return tenantView(tenantId);
    }

    public record TenantView(int passwordMinLength, int passwordHistoryCount,
                             int platformMinLength, int platformHistoryCount,
                             Map<OtpPurpose, OtpSettings> otp) {
    }

    private List<TenantSecurityPolicy> tenantPolicies(UserAccount account) {
        if (account.isPlatformAdmin()) {
            return List.of();
        }
        List<UUID> tenantIds = memberships.findByUserId(account.getId()).stream()
                .map(m -> m.getTenantId())
                .distinct()
                .toList();
        return tenantIds.isEmpty() ? List.of() : policies.findByTenantIdIn(tenantIds);
    }

    static OtpSettings fromJson(Map<String, Object> json, OtpSettings fallback) {
        if (json == null) {
            return fallback;
        }
        return new OtpSettings(
                Boolean.TRUE.equals(json.getOrDefault("enabled", fallback.enabled())),
                number(json, "length", fallback.length()),
                Duration.ofSeconds(number(json, "ttlSeconds", (int) fallback.ttl().toSeconds())),
                number(json, "maxAttempts", fallback.maxAttempts()),
                Duration.ofSeconds(number(json, "resendCooldownSeconds", (int) fallback.resendCooldown().toSeconds())));
    }

    static Map<String, Object> toJson(OtpSettings settings) {
        return Map.of(
                "enabled", settings.enabled(),
                "length", settings.length(),
                "ttlSeconds", settings.ttl().toSeconds(),
                "maxAttempts", settings.maxAttempts(),
                "resendCooldownSeconds", settings.resendCooldown().toSeconds());
    }

    private static int number(Map<String, Object> json, String key, int fallback) {
        return json.get(key) instanceof Number n ? n.intValue() : fallback;
    }
}
