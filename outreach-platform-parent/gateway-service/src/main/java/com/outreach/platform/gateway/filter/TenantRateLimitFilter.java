package com.outreach.platform.gateway.filter;

import com.outreach.platform.common.tenant.TenantConstants;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global gateway filter that enforces per-tenant rate limiting using Bucket4j token buckets.
 *
 * <p>Each tenant gets an independent token bucket stored in-memory (per instance).
 * The bucket is configured with a capacity and greedy refill rate controlled by
 * the {@code tenant.rate-limit.requests-per-second} property.</p>
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>If no {@code X-Tenant-ID} header is present (Platform_Admin), rate limiting is bypassed</li>
 *   <li>If the tenant's bucket is exhausted, returns HTTP 429 with {@code Retry-After} header</li>
 *   <li>Buckets are keyed by {@code ratelimit:{tenantId}}</li>
 * </ul>
 */
@Component
public class TenantRateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantRateLimitFilter.class);

    private static final String BUCKET_KEY_PREFIX = "ratelimit:";

    private final ConcurrentMap<String, Bucket> tenantBuckets = new ConcurrentHashMap<>();

    private final long requestsPerSecond;

    public TenantRateLimitFilter(
            @Value("${tenant.rate-limit.requests-per-second:100}") long requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
        log.info("TenantRateLimitFilter initialized with {} requests/second per tenant", requestsPerSecond);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders()
                .getFirst(TenantConstants.X_TENANT_ID_HEADER);

        // Platform_Admin requests have no X-Tenant-ID — bypass rate limiting
        if (tenantId == null || tenantId.isBlank()) {
            return chain.filter(exchange);
        }

        String bucketKey = BUCKET_KEY_PREFIX + tenantId;
        Bucket bucket = tenantBuckets.computeIfAbsent(bucketKey, key -> createBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            exchange.getResponse().getHeaders()
                    .add("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return chain.filter(exchange);
        }

        // Bucket exhausted — return 429
        long waitForRefillNanos = probe.getNanosToWaitForRefill();
        long retryAfterSeconds = Math.max(1, Duration.ofNanos(waitForRefillNanos).toSeconds());

        log.warn("Rate limit exceeded for tenant {}: retry after {}s", tenantId, retryAfterSeconds);

        return rejectWithTooManyRequests(exchange, tenantId, retryAfterSeconds);
    }

    private Bucket createBucket() {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(requestsPerSecond)
                .refillGreedy(requestsPerSecond, Duration.ofSeconds(1))
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private Mono<Void> rejectWithTooManyRequests(ServerWebExchange exchange,
                                                  String tenantId,
                                                  long retryAfterSeconds) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));
        response.getHeaders().add("X-RateLimit-Remaining", "0");

        String correlationId = exchange.getRequest().getHeaders()
                .getFirst("X-Correlation-ID");

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Tenant %s has exceeded the rate limit. Retry after %d seconds.\",\"correlationId\":\"%s\",\"fieldErrors\":{}}",
                Instant.now().toString(),
                tenantId,
                retryAfterSeconds,
                correlationId != null ? correlationId : ""
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run after TenantStatusValidationFilter (HIGHEST_PRECEDENCE + 101)
        return Ordered.HIGHEST_PRECEDENCE + 102;
    }
}
