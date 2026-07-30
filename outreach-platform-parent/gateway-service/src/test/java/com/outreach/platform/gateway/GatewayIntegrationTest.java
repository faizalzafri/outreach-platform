package com.outreach.platform.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Integration tests for the API Gateway.
 * Tests JWT validation, rate limiting, and circuit breaker behavior.
 *
 * Uses Testcontainers for Redis (started in a static initializer so it is
 * available when @DynamicPropertySource runs — which happens before @BeforeAll).
 * WireMock simulates the JWKS endpoint and downstream backend services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GatewayIntegrationTest.class);

    // All infrastructure must be started in a static initializer because
    // @DynamicPropertySource is resolved before @BeforeAll.
    private static final GenericContainer<?> redisContainer;
    private static final WireMockServer jwksServer;
    private static final WireMockServer backendServer;
    private static final RSAKey rsaKey;

    static {
        // Start Redis via Testcontainers
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redisContainer.start();
        log.info("Testcontainers Redis started on port {}", redisContainer.getMappedPort(6379));

        // Generate RSA key pair for JWT signing
        try {
            rsaKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key for tests", e);
        }

        // Start WireMock to serve the JWKS endpoint
        jwksServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        jwksServer.start();

        JWKSet jwkSet = new JWKSet(rsaKey.toPublicJWK());
        jwksServer.stubFor(get(urlPathEqualTo("/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jwkSet.toString())));

        // Start WireMock backend to simulate a downstream service for gateway routing
        backendServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        backendServer.start();

        backendServer.stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));
        backendServer.stubFor(post(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @AfterAll
    static void tearDown() {
        jwksServer.stop();
        backendServer.stop();
        redisContainer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Redis from Testcontainers (already started in static initializer)
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        // JWKS endpoint from WireMock
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + jwksServer.port() + "/protocol/openid-connect/certs");

        // Disable Eureka for testing
        registry.add("eureka.client.enabled", () -> "false");

        // Test route pointing to backend WireMock (for rate limiting and routing tests)
        registry.add("spring.cloud.gateway.routes[0].id", () -> "test-service");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:" + backendServer.port());
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/api/test/**");
        registry.add("spring.cloud.gateway.routes[0].filters[0]", () -> "StripPrefix=2");

        // Feedback route for JWT validation tests
        registry.add("spring.cloud.gateway.routes[1].id", () -> "feedback-service");
        registry.add("spring.cloud.gateway.routes[1].uri", () -> "http://localhost:" + backendServer.port());
        registry.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/api/feedback/**");
        registry.add("spring.cloud.gateway.routes[1].filters[0]", () -> "StripPrefix=2");
    }

    private String generateValidJwt() throws Exception {
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("http://localhost/realms/outreach")
                .audience("outreach-dashboard")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issueTime(Date.from(Instant.now()))
                .jwtID(UUID.randomUUID().toString())
                .claim("preferred_username", "testuser")
                .claim("realm_access", Map.of("roles", List.of("ROLE_ADMIN")))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private String generateExpiredJwt() throws Exception {
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("http://localhost/realms/outreach")
                .audience("outreach-dashboard")
                .expirationTime(Date.from(Instant.now().minusSeconds(3600))) // expired
                .issueTime(Date.from(Instant.now().minusSeconds(7200)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    @Nested
    @DisplayName("JWT Validation Tests")
    class JwtValidationTests {

        @Test
        @DisplayName("Should reject request with invalid JWT token with 401")
        void shouldRejectInvalidJwtWith401() {
            webTestClient.get()
                    .uri("/api/feedback/test")
                    .header("Authorization", "Bearer invalid.jwt.token")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Should reject request with expired JWT token with 401")
        void shouldRejectExpiredJwtWith401() throws Exception {
            String expiredToken = generateExpiredJwt();

            webTestClient.get()
                    .uri("/api/feedback/test")
                    .header("Authorization", "Bearer " + expiredToken)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Should reject request without Authorization header with 401")
        void shouldRejectMissingAuthWith401() {
            webTestClient.get()
                    .uri("/api/feedback/test")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Should allow access to permitted endpoints without authentication")
        void shouldAllowPermittedEndpointsWithoutAuth() {
            webTestClient.get()
                    .uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should allow access to fallback endpoint without authentication")
        void shouldAllowFallbackEndpointWithoutAuth() {
            webTestClient.get()
                    .uri("/fallback")
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }
    }

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @BeforeEach
        void clearRateLimitKeys() {
            // Clear any existing rate limit keys before each test
            redisTemplate.keys("rate_limit:unauth:*")
                    .flatMap(key -> redisTemplate.delete(key))
                    .blockLast(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("Should return 429 after exceeding unauthenticated rate limit of 20 req/min")
        void shouldReturn429AfterRateLimitExceeded() {
            // The AdaptiveRateLimitFilter limits unauthenticated requests to 20/min
            // Use /api/test/ping which is routed through the gateway (GlobalFilter applies)
            // and permitted without auth by the test security config
            for (int i = 0; i < 20; i++) {
                webTestClient.get()
                        .uri("/api/test/ping")
                        .exchange()
                        .expectStatus().isOk();
            }

            // The 21st request should be rate limited
            webTestClient.get()
                    .uri("/api/test/ping")
                    .exchange()
                    .expectStatus().isEqualTo(429)
                    .expectHeader().valueEquals("X-RateLimit-Limit", "20")
                    .expectHeader().valueEquals("X-RateLimit-Remaining", "0");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Tests")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Should return 503 structured response from fallback controller")
        void shouldReturn503FromFallbackController() {
            // The fallback endpoint is always accessible and returns 503
            webTestClient.get()
                    .uri("/fallback")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(503)
                    .jsonPath("$.error").isEqualTo("Service Unavailable")
                    .jsonPath("$.message").isNotEmpty()
                    .jsonPath("$.timestamp").isNotEmpty()
                    .jsonPath("$.path").isEqualTo("/fallback");
        }

        @Test
        @DisplayName("Should return 503 with correct content type from fallback")
        void shouldReturnJsonContentTypeFromFallback() {
            webTestClient.post()
                    .uri("/fallback")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectHeader().contentType("application/json")
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(503)
                    .jsonPath("$.error").isEqualTo("Service Unavailable");
        }
    }
}
