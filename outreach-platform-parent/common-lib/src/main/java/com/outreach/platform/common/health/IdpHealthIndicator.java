package com.outreach.platform.common.health;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Custom health indicator that verifies the Identity Provider's JWKS endpoint
 * is reachable and returning valid JSON.
 *
 * <p>Only activated when {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}
 * is configured. Performs a lightweight GET request to the JWKS URI and validates
 * the response contains a {@code keys} array (standard JWKS response structure).
 *
 * <p>Reports UP with the JWKS URI or DOWN with the failure reason.
 */
@Component
@ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
public class IdpHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(IdpHealthIndicator.class);

    private final String jwksUri;
    private final RestClient restClient;

    @Inject
    public IdpHealthIndicator(
            org.springframework.core.env.Environment environment,
            org.springframework.beans.factory.ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
        this.jwksUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
        RestClient.Builder builder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        this.restClient = builder.build();
    }

    @Override
    public Health health() {
        if (jwksUri == null || jwksUri.isBlank()) {
            return Health.unknown()
                    .withDetail("reason", "JWKS URI not configured")
                    .build();
        }

        try {
            String response = restClient.get()
                    .uri(jwksUri)
                    .retrieve()
                    .body(String.class);

            if (response != null && response.contains("\"keys\"")) {
                return Health.up()
                        .withDetail("jwksUri", jwksUri)
                        .build();
            }

            return Health.down()
                    .withDetail("jwksUri", jwksUri)
                    .withDetail("error", "Invalid JWKS response — missing 'keys' field")
                    .build();
        } catch (Exception ex) {
            log.warn("IdP health check failed for {}: {}", jwksUri, ex.getMessage());
            return Health.down()
                    .withDetail("jwksUri", jwksUri)
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
