package com.outreach.platform.common.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Registers {@link ResilientCacheErrorHandler} for every service, regardless of which
 * {@link CacheManager} it wires up, so a poisoned cache entry never turns into a standing 500.
 */
@AutoConfiguration
@ConditionalOnClass(CacheManager.class)
@ConditionalOnMissingBean(CachingConfigurer.class)
public class CacheErrorHandlingAutoConfiguration implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }
}
