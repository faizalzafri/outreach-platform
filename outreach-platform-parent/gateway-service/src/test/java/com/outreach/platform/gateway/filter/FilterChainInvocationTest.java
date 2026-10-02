package com.outreach.platform.gateway.filter;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.gateway.service.TenantStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Each global filter must hand the request to the rest of the chain exactly once. A successful
 * chain completes empty, so {@code flatMap(... chain.filter ...).switchIfEmpty(chain.filter ...)}
 * re-runs the whole downstream chain after the response is already written.
 */
class FilterChainInvocationTest {

    private final AtomicInteger chainCalls = new AtomicInteger();
    private final GatewayFilterChain countingChain = exchange -> {
        chainCalls.incrementAndGet();
        return Mono.empty();
    };

    @Test
    void adaptiveRateLimit_authenticatedRequest_callsChainOnce() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class, RETURNS_DEEP_STUBS);
        when(redis.opsForValue().increment(anyString())).thenReturn(Mono.just(1L));
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        new AdaptiveRateLimitFilter(redis).filter(authenticated(jwt(UUID.randomUUID())), countingChain)
                .block(Duration.ofSeconds(5));

        assertThat(chainCalls).hasValue(1);
    }

    @Test
    void tenantExtraction_authenticatedRequest_callsChainOnce() {
        new TenantExtractionFilter().filter(authenticated(jwt(UUID.randomUUID())), countingChain).block(Duration.ofSeconds(5));

        assertThat(chainCalls).hasValue(1);
    }

    @Test
    void tenantExtraction_anonymousRequest_callsChainOnce() {
        new TenantExtractionFilter().filter(anonymous(), countingChain).block(Duration.ofSeconds(5));

        assertThat(chainCalls).hasValue(1);
    }

    @Test
    void tenantStatusValidation_downstreamError_propagatesWithoutRerunningChain() {
        TenantStatusService statusService = mock(TenantStatusService.class);
        when(statusService.getTenantStatus(anyString())).thenReturn(Mono.just(TenantStatusService.STATUS_ACTIVE));
        GatewayFilterChain failingChain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.error(new IllegalStateException("downstream failed"));
        };

        Mono<Void> result = new TenantStatusValidationFilter(statusService).filter(withTenantHeader(), failingChain);

        assertThat(result.onErrorResume(e -> Mono.empty()).block()).isNull();
        assertThat(chainCalls).hasValue(1);
    }

    @Test
    void tenantStatusValidation_statusLookupFailure_failsOpenOnce() {
        TenantStatusService statusService = mock(TenantStatusService.class);
        when(statusService.getTenantStatus(anyString())).thenReturn(Mono.error(new IllegalStateException("redis down")));

        new TenantStatusValidationFilter(statusService).filter(withTenantHeader(), countingChain).block(Duration.ofSeconds(5));

        assertThat(chainCalls).hasValue(1);
    }

    private static ServerWebExchange authenticated(Jwt jwt) {
        return anonymous().mutate().principal(Mono.just(new JwtAuthenticationToken(jwt))).build();
    }

    private static ServerWebExchange anonymous() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/events").remoteAddress(
                new java.net.InetSocketAddress("127.0.0.1", 50000)));
    }

    private static ServerWebExchange withTenantHeader() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/events")
                .header(TenantConstants.X_TENANT_ID_HEADER, UUID.randomUUID().toString()));
    }

    private static Jwt jwt(UUID tenantId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin")
                .claim("tenant_id", tenantId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
