package com.outreach.platform.common.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.UUID;

/** Tracks and bulk-invalidates tenant-scoped cache entries in Redis using per-tenant key Sets. */
public class TenantCacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(TenantCacheInvalidationService.class);

    private static final String TRACKING_KEY_FORMAT = "tenant:%s:_keys";

    private final StringRedisTemplate redisTemplate;

    public TenantCacheInvalidationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Registers a cache key as belonging to the specified tenant.
     *
     * @param tenantId the tenant that owns the cache entry
     * @param cacheKey the full cache key to track
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
     * Deletes all cached entries for the specified tenant and removes the tracking Set.
     *
     * @param tenantId the tenant whose cache entries should be invalidated
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

        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (String key : trackedKeys) {
                connection.keyCommands().del(key.getBytes());
            }
            return null;
        });

        redisTemplate.delete(trackingKey);

        log.info("Successfully invalidated {} cache entries for tenant [{}]", keyCount, tenantId);
        return keyCount;
    }

    private String buildTrackingKey(UUID tenantId) {
        return String.format(TRACKING_KEY_FORMAT, tenantId);
    }
}
