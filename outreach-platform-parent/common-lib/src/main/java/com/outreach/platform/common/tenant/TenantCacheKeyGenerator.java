package com.outreach.platform.common.tenant;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Tenant-aware cache key generator that prefixes all cache keys with the current tenant ID.
 * <p>
 * This ensures complete cache isolation between tenants when using shared Redis infrastructure.
 * Keys are generated in the format:
 * <pre>
 *   tenant:{tenantId}:{className}.{methodName}:{paramsHash}
 * </pre>
 * <p>
 * When no tenant context is present (e.g., Platform_Admin operating in cross-tenant mode),
 * keys omit the tenant prefix:
 * <pre>
 *   {className}.{methodName}:{paramsHash}
 * </pre>
 * <p>
 * The params portion uses {@link Arrays#deepHashCode(Object[])} to produce a stable hash
 * of the method arguments, avoiding excessively long cache keys while maintaining uniqueness.
 *
 * @see TenantContext
 * @see org.springframework.cache.interceptor.KeyGenerator
 */
public class TenantCacheKeyGenerator implements KeyGenerator {

    /**
     * Generates a tenant-scoped cache key based on the target class, method, and parameters.
     *
     * @param target the target object on which the cached method is invoked
     * @param method the cached method being called
     * @param params the method parameters used as part of the cache key
     * @return a tenant-prefixed cache key string, or an unprefixed key if no tenant context is set
     */
    @Override
    public Object generate(Object target, Method method, Object... params) {
        String className = target.getClass().getSimpleName();
        String methodName = method.getName();
        int paramsHash = Arrays.deepHashCode(params);

        String baseKey = className + "." + methodName + ":" + paramsHash;

        if (TenantContext.isPresent()) {
            return "tenant:" + TenantContext.getCurrentTenantId() + ":" + baseKey;
        }

        return baseKey;
    }
}
