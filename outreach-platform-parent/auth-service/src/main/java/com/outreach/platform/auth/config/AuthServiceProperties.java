package com.outreach.platform.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

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
        @NotBlank String issuerUri
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
     * Security policy configuration for password and brute-force protection.
     */
    public record SecurityProperties(
            PasswordPolicy passwordPolicy,
            BruteForceProtection bruteForce
    ) {
        public record PasswordPolicy(
                @Min(1) int minLength,
                boolean requireUppercase,
                boolean requireLowercase,
                boolean requireDigit,
                boolean requireSpecialChar
        ) {}

        public record BruteForceProtection(
                @Min(1) int maxFailedAttempts,
                Duration lockDuration
        ) {}
    }
}
