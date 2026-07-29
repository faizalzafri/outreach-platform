package com.outreach.platform.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service responsible for resolving tenant status using a two-tier cache strategy:
 * <ol>
 *   <li><b>L1 — Caffeine (local, 5s TTL, max 1000 entries)</b>: eliminates network roundtrip for hot-path reads</li>
 *   <li><b>L2 — Redis (60s TTL)</b>: shared cache across gateway instances</li>
 *   <li><b>L3 — Database</b>: source of truth (fallback, not yet wired)</li>
 * </ol>
 * <p>
 * Pattern: Caffeine (5s) → Redis (60s) → Database
 * This reduces Redis load by ~90% for hot-path reads (every request hits tenant status).
 */
@Service
public class TenantStatusService {

    private static final Logger log = LoggerFactory.getLogger(TenantStatusService.class);

    private static final String CACHE_KEY_PREFIX = "tenant:status:";
    private static final Duration REDIS_CACHE_TTL = Duration.ofSeconds(60);

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_DEACTIVATED = "DEACTIVATED";

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * L1 local cache: Caffeine with 5-second TTL and bounded size of 1000 entries.
     * Short TTL ensures status changes propagate within seconds while eliminating
     * a Redis network roundtrip on every gateway request.
     */
    private final Cache<String, String> localCache;

    public TenantStatusService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(5))
                .recordStats()
                .build();
    }

    /**
     * Resolves the status of the given tenant using two-tier caching.
     * <p>
     * Lookup order:
     * <ol>
     *   <li>Check Caffeine local cache (L1) — no network call</li>
     *   <li>On L1 miss: check Redis (L2) — one network call</li>
     *   <li>On L2 miss: resolve from DB (L3) — currently defaults to ACTIVE</li>
     * </ol>
     *
     * @param tenantId the tenant identifier
     * @return a Mono emitting the tenant status string (ACTIVE, SUSPENDED, or DEACTIVATED)
     */
    public Mono<String> getTenantStatus(String tenantId) {
        // L1: Check Caffeine local cache first (no network roundtrip)
        String cachedLocally = localCache.getIfPresent(tenantId);
        if (cachedLocally != null) {
            log.debug("Tenant status L1 cache hit for {}: {}", tenantId, cachedLocally);
            return Mono.just(cachedLocally);
        }

        // L2: Fall through to Redis
        String cacheKey = CACHE_KEY_PREFIX + tenantId;
        return redisTemplate.opsForValue().get(cacheKey)
                .doOnNext(status -> {
                    log.debug("Tenant status L2 (Redis) cache hit for {}: {}", tenantId, status);
                    // Populate L1 cache on Redis hit
                    localCache.put(tenantId, status);
                })
                .switchIfEmpty(resolveAndCache(tenantId, cacheKey));
    }

    /**
     * Resolves tenant status on L1+L2 cache miss. Currently defaults to ACTIVE.
     * In the future, this will call an internal endpoint (e.g., auth-service) to fetch the actual status.
     */
    private Mono<String> resolveAndCache(String tenantId, String cacheKey) {
        log.debug("Tenant status L1+L2 cache miss for {}; defaulting to ACTIVE", tenantId);

        // TODO: Replace with WebClient call to internal tenant status endpoint
        String resolvedStatus = STATUS_ACTIVE;

        // Populate both L1 and L2 caches
        localCache.put(tenantId, resolvedStatus);

        return redisTemplate.opsForValue()
                .set(cacheKey, resolvedStatus, REDIS_CACHE_TTL)
                .thenReturn(resolvedStatus);
    }

    /**
     * Invalidates the L1 local cache entry for a specific tenant.
     * Called when a tenant status change event is received.
     *
     * @param tenantId the tenant identifier to invalidate
     */
    public void invalidateLocalCache(String tenantId) {
        localCache.invalidate(tenantId);
        log.debug("Invalidated L1 cache for tenant {}", tenantId);
    }

    /**
     * Returns the Caffeine cache stats for monitoring/actuator purposes.
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getLocalCacheStats() {
        return localCache.stats();
    }
}
