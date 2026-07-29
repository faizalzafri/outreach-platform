package com.outreach.platform.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service responsible for resolving tenant status from Redis cache.
 * <p>
 * Lookup strategy:
 * <ol>
 *   <li>Check Redis for key {@code tenant:status:{tenantId}}</li>
 *   <li>On cache miss: assume ACTIVE (fallback to DB query via internal call will be wired later)</li>
 *   <li>Cache the resolved status in Redis with a 60-second TTL</li>
 * </ol>
 */
@Service
public class TenantStatusService {

    private static final Logger log = LoggerFactory.getLogger(TenantStatusService.class);

    private static final String CACHE_KEY_PREFIX = "tenant:status:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_DEACTIVATED = "DEACTIVATED";

    private final ReactiveStringRedisTemplate redisTemplate;

    public TenantStatusService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Resolves the status of the given tenant.
     * <p>
     * First checks Redis cache. On cache miss, falls back to ACTIVE (the DB fallback
     * via an internal service call will be implemented in a future task).
     *
     * @param tenantId the tenant identifier
     * @return a Mono emitting the tenant status string (ACTIVE, SUSPENDED, or DEACTIVATED)
     */
    public Mono<String> getTenantStatus(String tenantId) {
        String cacheKey = CACHE_KEY_PREFIX + tenantId;

        return redisTemplate.opsForValue().get(cacheKey)
                .doOnNext(status -> log.debug("Tenant status cache hit for {}: {}", tenantId, status))
                .switchIfEmpty(resolveAndCache(tenantId, cacheKey));
    }

    /**
     * Resolves tenant status on cache miss. Currently defaults to ACTIVE.
     * In the future, this will call an internal endpoint (e.g., auth-service) to fetch the actual status.
     */
    private Mono<String> resolveAndCache(String tenantId, String cacheKey) {
        log.debug("Tenant status cache miss for {}; defaulting to ACTIVE", tenantId);

        // TODO: Replace with WebClient call to internal tenant status endpoint
        String resolvedStatus = STATUS_ACTIVE;

        return redisTemplate.opsForValue()
                .set(cacheKey, resolvedStatus, CACHE_TTL)
                .thenReturn(resolvedStatus);
    }
}
