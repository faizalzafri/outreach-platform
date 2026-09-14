package com.outreach.platform.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * A cache entry that was written under an older application version (a changed DTO shape, or a
 * changed serializer config) can become undeserializable by the current code without ever
 * expiring first. The default {@link CacheErrorHandler} lets that failure propagate as an
 * unhandled exception from the cache lookup, turning every call for that key into a 500 until the
 * entry's TTL happens to elapse. Evicting the offending key and falling through to the method body
 * instead treats the failure as a cache miss, self-healing on the very next request.
 */
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ResilientCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache get failed for cache '{}' key '{}', evicting and falling through to source",
                cache.getName(), key, exception);
        cache.evictIfPresent(key);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache put failed for cache '{}' key '{}'", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache evict failed for cache '{}' key '{}'", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache clear failed for cache '{}'", cache.getName(), exception);
    }
}
