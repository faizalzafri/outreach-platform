package com.outreach.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared identity provider configuration properties.
 *
 * <p><strong>IdP Switching Mechanism:</strong> The platform supports two interchangeable
 * identity providers selected via the {@code idp.provider} property:
 * <ul>
 *   <li>{@code keycloak} — Keycloak Docker container (production-recommended)</li>
 *   <li>{@code spring} — Embedded Spring Authorization Server (lightweight alternative)</li>
 * </ul>
 *
 * <p>The {@code idp.jwks-uri} property configures the JWKS endpoint that resource servers
 * use to validate JWT access tokens. This should resolve to:
 * <ul>
 *   <li>Keycloak: {@code http://localhost:8080/realms/outreach/protocol/openid-connect/certs}</li>
 *   <li>Spring:   {@code http://localhost:8090/oauth2/jwks}</li>
 * </ul>
 *
 * <p>Resource servers always validate tokens via JWKS regardless of which provider is active,
 * ensuring consistent security posture across provider switches (Requirement 2.16).
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7517">RFC 7517 - JSON Web Key (JWK)</a>
 */
@ConfigurationProperties(prefix = "idp")
@Validated
public record IdpProperties(
        /**
         * Active identity provider. Determines which IdP issues tokens.
         * Valid values: {@code keycloak}, {@code spring}.
         */
        @NotBlank String provider,

        /**
         * JWKS endpoint URI for token signature verification.
         * Resource servers use this to fetch the public keys for JWT validation.
         */
        @NotBlank String jwksUri
) {
}
