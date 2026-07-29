package com.outreach.platform.common.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for tracking and invalidating tenant-scoped cache entries in Redis.
 * <p>
 * Uses a Redis Set per tenant ({@code tenant:{tenantId}:_keys}) to track all cache keys
 * belonging to that tenant. This design avoids the {@code KEYS} command (which performs an
 * O(N) full keyspace scan that blocks Redis under load) and provides O(M) invalidation
 * where M is the number of that tenant's cache entries.
 * <p>
 * <strong>Key Tracking:</strong> After any cache write operation, call {@link #trackKey(UUID, String)}
 * to register the cache key in the tenant's tracking Set via {@code SADD}.
 * <p>
 * <strong>Bulk Invalidation:</strong> When a tenant is deactivated, call
 * {@link #invalidateAllForTenant(UUID)} which reads all members from the tracking Set
 * via {@code SMEMBERS}, deletes them in a pipeline batch, then deletes the tracking Set itself.
 *
 * @see TenantCacheKeyGenerator
 * @see TenantContext
 */
public class TenantCacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(TenantCacheInvalidationService.class);

    /**
     * Format for the Redis Set key that tracks all cache keys belonging to a tenant.
     * The placeholder is replaced with the tenant's UUID.
     */
    private static final String TRACKING_KEY_FORMAT = "tenant:%s:_keys";

    private final StringRedisTemplate redisTemplate;

    /**
     * Creates a new {@code TenantCacheInvalidationService} backed by the provided Redis template.
     *
     * @param redisTemplate the {@link StringRedisTemplate} used for all Redis operations
     */
    public TenantCacheInvalidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tracks a cache key as belonging to the specified tenant.
     * <p>
     * Adds the given {@code cacheKey} to the Redis Set at {@code tenant:{tenantId}:_keys}
     * using the {@code SADD} command. This operation is idempotent — adding the same key
     * multiple times has no effect on the Set.
     *
     * @param tenantId the UUID of the tenant that owns the cache entry
     * @param cacheKey the full cache key to track (e.g., {@code tenant:uuid:ClassName.method:hash})
     */
    public void trackKey(UUID tenantId, String cacheKey) {
        if (tenantId == null || cacheKey == null || cacheKey.isBlank()) {
            return;
        }
        String trackingKey = buildTrackingKey(tenantId);
        redisTemplate.opsForSet().add(trackingKey, cacheKey);
        log.debug("Tracked cache key [{}] for tenant [{}]", cacheKey, tenantId);
    }

    /**
     * Invalidates all cached entries for the specified tenant and removes the tracking Set.
     * <p>
     * Performs the following steps:
     * <ol>
     *   <li>Reads all members from {@code tenant:{tenantId}:_keys} via {@code SMEMBERS}</li>
     *   <li>Deletes all tracked cache keys in a Redis pipeline batch for efficiency</li>
     *   <li>Deletes the tracking Set itself</li>
     * </ol>
     * <p>
     * This provides O(M) invalidation where M is the number of that tenant's cache entries,
     * avoiding the O(N) full keyspace scan that {@code KEYS tenant:*} would require.
     *
     * @param tenantId the UUID of the tenant whose cache entries should be invalidated
     * @return the number of cache keys that were invalidated
     */
    public long invalidateAllForTenant(UUID tenantId) {
        if (tenantId == null) {
            return 0;
        }

        String trackingKey = buildTrackingKey(tenantId);
        Set<String> trackedKeys = redisTemplate.opsForSet().members(trackingKey);

        if (trackedKeys == null || trackedKeys.isEmpty()) {
            log.info("No cached keys found for tenant [{}], nothing to invalidate", tenantId);
            redisTemplate.delete(trackingKey);
            return 0;
        }

        long keyCount = trackedKeys.size();
        log.info("Invalidating {} cache entries for tenant [{}]", keyCount, tenantId);

        // Delete all tracked keys in a pipeline batch for efficiency
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (String key : trackedKeys) {
                connection.keyCommands().del(key.getBytes());
            }
            return null;
        });

        // Delete the tracking Set itself
        redisTemplate.delete(trackingKey);

        log.info("Successfully invalidated {} cache entries for tenant [{}]", keyCount, tenantId);
        return keyCount;
    }

    /**
     * Builds the Redis Set key used to track all cache keys for a given tenant.
     *
     * @param tenantId the tenant's UUID
     * @return the tracking key in the format {@code tenant:{tenantId}:_keys}
     */
    private String buildTrackingKey(UUID tenantId) {
        return String.format(TRACKING_KEY_FORMAT, tenantId);
    }
}
