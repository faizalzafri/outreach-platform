package com.outreach.platform.auth.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

/**
 * Type-safe configuration properties for the embedded Authorization Server.
 * Mirrors the Keycloak realm settings for consistent behavior across providers.
 *
 * <p>These properties are only needed when {@code idp.provider=spring}. They are
 * registered via {@link AuthorizationServerConfig} which is itself conditional on
 * that property value.
 */
@ConfigurationProperties(prefix = "auth-server")
@Validated
public record AuthServiceProperties(
        ClientsProperties clients,
        TokenProperties token,
        SecurityProperties security,
        @NotBlank String issuerUri,
        /** Base URL of this server as a browser reaches it; used in emailed links. */
        @DefaultValue("http://localhost:8090") String publicUrl,
        @DefaultValue AccountProperties accounts,
        @DefaultValue BootstrapProperties bootstrap
) {

    /**
     * Client registration settings.
     */
    public record ClientsProperties(
            DashboardClient dashboard,
            ServiceClient services
    ) {
        public record DashboardClient(
                @NotBlank String clientId,
                @NotBlank String redirectUri,
                @NotBlank String silentRedirectUri,
                @NotBlank String postLogoutRedirectUri
        ) {}

        public record ServiceClient(
                @NotBlank String clientId,
                @NotBlank String clientSecret
        ) {}
    }

    /**
     * Token lifetime configuration.
     */
    public record TokenProperties(
            Duration accessTokenTimeToLive,
            Duration refreshTokenTimeToLive,
            @Min(0) int refreshTokenMaxReuse
    ) {}

    /**
     * Security policy configuration: platform defaults for passwords, brute-force protection and
     * one-time passcodes. Tenants may tighten the password policy and override OTP settings.
     */
    public record SecurityProperties(
            PasswordPolicy passwordPolicy,
            BruteForceProtection bruteForce,
            @DefaultValue OtpDefaults otp
    ) {
        public record PasswordPolicy(
                @Min(1) int minLength,
                boolean requireUppercase,
                boolean requireLowercase,
                boolean requireDigit,
                boolean requireSpecialChar,
                /** How many previous passwords a new one may not repeat. */
                @DefaultValue("5") @Min(0) int historyCount,
                @DefaultValue("true") boolean rejectCommonPasswords
        ) {}

        public record BruteForceProtection(
                @Min(1) int maxFailedAttempts,
                Duration lockDuration
        ) {}

        /**
         * Platform defaults per OTP purpose ({@code LOGIN}, {@code PASSWORD_RESET},
         * {@code PASSWORD_CHANGE}). A purpose missing here falls back to {@link OtpSettings#DISABLED}.
         */
        public record OtpDefaults(@DefaultValue Map<String, OtpSettings> purposes) {}

        public record OtpSettings(
                boolean enabled,
                @DefaultValue("6") @Min(6) @Max(8) int length,
                @DefaultValue("5m") Duration ttl,
                @DefaultValue("5") @Min(1) int maxAttempts,
                @DefaultValue("30s") Duration resendCooldown
        ) {
            public static final OtpSettings DISABLED =
                    new OtpSettings(false, 6, Duration.ofMinutes(5), 5, Duration.ofSeconds(30));
        }
    }

    /** Lifetimes of the single-use links sent by email. */
    public record AccountProperties(
            @DefaultValue("72h") Duration activationTtl,
            @DefaultValue("30m") Duration passwordResetTtl
    ) {}

    /** Creates the first platform admin, by invitation, when none exists. */
    public record BootstrapProperties(
            String platformAdminEmail,
            @DefaultValue("platform_admin") String platformAdminUsername
    ) {}
}
