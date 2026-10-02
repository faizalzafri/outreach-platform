package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.config.AuthServiceProperties.SecurityProperties.OtpSettings;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.service.SecurityPolicyService;
import com.outreach.platform.auth.service.SecurityPolicyService.TenantView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * An organization's sign-in security: password minimum and history, and which actions need a
 * one-time code (and how it behaves). Tenant admins manage their own organization; platform admins
 * name one with {@code tenantId}. Values can only be at least as strict as the platform defaults.
 */
@RestController
@RequestMapping("/api/auth/security-policy")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM_ADMIN')")
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
@Tag(name = "Security Policy", description = "Password and one-time-code settings for an organization")
public class SecurityPolicyController {

    private final SecurityPolicyService policies;

    @Inject
    public SecurityPolicyController(SecurityPolicyService policies) {
        this.policies = policies;
    }

    public record OtpSettingsDto(
            boolean enabled,
            @Min(6) @Max(8) int length,
            @Min(60) @Max(900) int ttlSeconds,
            @Min(1) @Max(10) int maxAttempts,
            @Min(0) @Max(300) int resendCooldownSeconds) {

        static OtpSettingsDto of(OtpSettings settings) {
            return new OtpSettingsDto(settings.enabled(), settings.length(), (int) settings.ttl().toSeconds(),
                    settings.maxAttempts(), (int) settings.resendCooldown().toSeconds());
        }

        OtpSettings toSettings() {
            return new OtpSettings(enabled, length, Duration.ofSeconds(ttlSeconds), maxAttempts,
                    Duration.ofSeconds(resendCooldownSeconds));
        }
    }

    public record PolicyDto(
            @Min(8) @Max(128) int passwordMinLength,
            @Min(0) @Max(24) int passwordHistoryCount,
            Integer platformMinLength,
            Integer platformHistoryCount,
            @NotNull Map<OtpPurpose, @Valid OtpSettingsDto> otp) {

        static PolicyDto of(TenantView view) {
            Map<OtpPurpose, OtpSettingsDto> otp = new EnumMap<>(OtpPurpose.class);
            view.otp().forEach((purpose, settings) -> otp.put(purpose, OtpSettingsDto.of(settings)));
            return new PolicyDto(view.passwordMinLength(), view.passwordHistoryCount(),
                    view.platformMinLength(), view.platformHistoryCount(), otp);
        }
    }

    @Operation(summary = "The organization's security policy")
    @GetMapping
    public PolicyDto get(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID tenantId) {
        return PolicyDto.of(policies.tenantView(Caller.of(jwt).tenantFor(tenantId)));
    }

    @Operation(summary = "Change the organization's security policy")
    @PutMapping
    public PolicyDto update(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID tenantId,
                            @Valid @RequestBody PolicyDto request) {
        Map<OtpPurpose, OtpSettings> otp = new EnumMap<>(OtpPurpose.class);
        request.otp().forEach((purpose, dto) -> otp.put(purpose, dto.toSettings()));
        return PolicyDto.of(policies.update(Caller.of(jwt).tenantFor(tenantId),
                request.passwordMinLength(), request.passwordHistoryCount(), otp));
    }
}
