package com.outreach.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for the active identity provider (keycloak or spring) and its JWKS URI.
 */
@ConfigurationProperties(prefix = "idp")
@Validated
public record IdpProperties(
        @NotBlank String provider,
        @NotBlank String jwksUri
) {
}
