package com.outreach.platform.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/** Resolves tenant status using two-tier caching: Caffeine (L1, 5s) → Redis (L2, 60s) → Database (L3). */
@Service
public class TenantStatusService {

    private static final Logger log = LoggerFactory.getLogger(TenantStatusService.class);

    private static final String CACHE_KEY_PREFIX = "tenant:status:";
    private static final Duration REDIS_CACHE_TTL = Duration.ofSeconds(60);

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_DEACTIVATED = "DEACTIVATED";

    private final ReactiveStringRedisTemplate redisTemplate;

    /** L1 local cache: Caffeine with 5s TTL, max 1000 entries. */
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
     * Resolves the tenant status using L1 (Caffeine) → L2 (Redis) → L3 (DB) lookup.
     *
     * @param tenantId the tenant identifier
     * @return a Mono emitting the tenant status string
     */
    public Mono<String> getTenantStatus(String tenantId) {
        // L1: Check local cache first
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

    /** Resolves tenant status on cache miss; currently defaults to ACTIVE. */
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
     * Invalidates the L1 local cache entry for a tenant.
     *
     * @param tenantId the tenant identifier to invalidate
     */
    public void invalidateLocalCache(String tenantId) {
        localCache.invalidate(tenantId);
        log.debug("Invalidated L1 cache for tenant {}", tenantId);
    }

    /** Returns the Caffeine cache stats for monitoring. */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getLocalCacheStats() {
        return localCache.stats();
    }
}
