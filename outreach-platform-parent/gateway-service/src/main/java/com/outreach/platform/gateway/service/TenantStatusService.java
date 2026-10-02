package com.outreach.platform.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/** Resolves tenant status: Caffeine (L1, 5s) → Redis (L2, 60s) → auth-service, which owns tenants. */
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

    private final WebClient webClient;
    private final String statusUri;

    public TenantStatusService(ReactiveStringRedisTemplate redisTemplate,
                               ReactorLoadBalancerExchangeFilterFunction loadBalancer,
                               @Value("${gateway.tenant-status-uri:http://auth-service/internal/tenants/{id}/status}") String statusUri) {
        this.redisTemplate = redisTemplate;
        this.webClient = WebClient.builder().filter(loadBalancer).build();
        this.statusUri = statusUri;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(5))
                .recordStats()
                .build();
    }

    /**
     * Resolves the tenant status using L1 (Caffeine) → L2 (Redis) → auth-service.
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
                // defer: building the fallback eagerly puts ACTIVE into L1 before Redis has answered
                .switchIfEmpty(Mono.defer(() -> resolveAndCache(tenantId, cacheKey)));
    }

    /**
     * Cache miss: ask auth-service, which owns tenants. If it cannot be reached, the request is let
     * through as ACTIVE (and not cached, so the next request asks again) so an auth-service outage doesn't take every
     * tenant offline; a tenant auth-service reports as suspended or deactivated is refused.
     */
    private Mono<String> resolveAndCache(String tenantId, String cacheKey) {
        return webClient.get()
                .uri(statusUri, tenantId)
                .retrieve()
                .bodyToMono(StatusResponse.class)
                .map(StatusResponse::status)
                .timeout(Duration.ofSeconds(2))
                .flatMap(status -> {
                    log.debug("Tenant status resolved from auth-service for {}: {}", tenantId, status);
                    localCache.put(tenantId, status);
                    return redisTemplate.opsForValue().set(cacheKey, status, REDIS_CACHE_TTL).thenReturn(status);
                })
                .onErrorResume(e -> {
                    log.warn("Tenant status lookup failed for {}; allowing as ACTIVE: {}", tenantId, e.toString());
                    return Mono.just(STATUS_ACTIVE);
                });
    }

    private record StatusResponse(String status) {
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
