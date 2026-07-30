package com.outreach.platform.common.tenant;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Cache key generator that prefixes keys with the current tenant ID for cache isolation. */
public class TenantCacheKeyGenerator implements KeyGenerator {

    /**
     * Generates a tenant-scoped cache key in format: tenant:{tenantId}:{class}.{method}:{paramsHash}.
     *
     * @param target the target object
     * @param method the cached method
     * @param params the method parameters
     * @return a tenant-prefixed cache key, or unprefixed if no tenant context is set
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
