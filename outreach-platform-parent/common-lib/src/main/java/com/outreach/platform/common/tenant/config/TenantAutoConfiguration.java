package com.outreach.platform.common.tenant.config;

import com.outreach.platform.common.tenant.TenantCacheKeyGenerator;
import com.outreach.platform.common.tenant.TenantContextFilter;
import com.outreach.platform.common.tenant.TenantContextTaskDecorator;
import com.outreach.platform.common.tenant.TenantFilterAspect;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-configuration that registers tenant infrastructure components for downstream services.
 * <p>
 * Activates when {@code tenant.enabled} property is {@code true} (defaults to {@code true}).
 * Services that do not need tenant context (e.g., config utilities) can opt out by setting
 * {@code tenant.enabled=false} in their application configuration.
 * <p>
 * Registers:
 * <ul>
 *   <li>{@link TenantContextFilter} — servlet filter extracting {@code X-Tenant-ID} header into thread-local context</li>
 *   <li>{@link TenantFilterAspect} — AOP aspect enabling Hibernate tenant filter on repository calls (conditional on JPA)</li>
 *   <li>{@link TenantContextTaskDecorator} — propagates tenant context to async executor threads</li>
 *   <li>{@link ThreadPoolTaskExecutorCustomizer} — applies the task decorator to the default {@code ThreadPoolTaskExecutor}</li>
 *   <li>{@link TenantCacheKeyGenerator} — default {@link KeyGenerator} that prefixes cache keys with tenant ID (conditional on Spring Cache)</li>
 *   <li>TenantMessagePostProcessor — adds {@code x-tenant-id} header to outbound RabbitMQ messages (conditional on Spring AMQP)</li>
 *   <li>TenantMessageInterceptor — extracts {@code x-tenant-id} header from inbound RabbitMQ messages into tenant context (conditional on Spring AMQP)</li>
 *   <li>TenantCacheInvalidationService — tracks and bulk-invalidates tenant-scoped cache entries using per-tenant Redis Sets (conditional on {@code StringRedisTemplate} bean)</li>
 * </ul>
 *
 * @see TenantContextFilter
 * @see TenantFilterAspect
 * @see TenantContextTaskDecorator
 */
@AutoConfiguration
@ConditionalOnProperty(name = "tenant.enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantAutoConfiguration.class);

    /**
     * Lifecycle hook invoked during graceful shutdown.
     * Logs an informational message indicating that active tenant contexts are being drained.
     * This is non-blocking — it does not wait for contexts to clear, since the container's
     * graceful shutdown mechanism handles in-flight request completion.
     */
    @PreDestroy
    public void onShutdown() {
        log.info("Graceful shutdown: draining active tenant contexts");
    }

    /**
     * Registers {@link TenantContextFilter} as a servlet filter with high precedence.
     * <p>
     * Order is {@code Ordered.HIGHEST_PRECEDENCE + 10} — after correlation ID filters
     * but before Spring Security filter chain, ensuring tenant context is available
     * for security evaluations and all downstream processing.
     */
    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration() {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("tenantContextFilter");
        return registration;
    }

    /**
     * Registers {@link TenantFilterAspect} that enables Hibernate session-level tenant
     * filtering before JPA repository method execution.
     * <p>
     * Only activated when an {@link EntityManager} bean is present in the application context
     * (i.e., JPA auto-configuration has run and created one).
     */
    @Bean
    @ConditionalOnBean(EntityManager.class)
    public TenantFilterAspect tenantFilterAspect(EntityManager entityManager) {
        return new TenantFilterAspect(entityManager);
    }

    /**
     * Registers {@link TenantContextTaskDecorator} as a bean for use by async executors
     * and any custom thread pool that needs tenant context propagation.
     */
    @Bean
    public TenantContextTaskDecorator tenantContextTaskDecorator() {
        return new TenantContextTaskDecorator();
    }

    /**
     * Customizes the default {@code ThreadPoolTaskExecutor} used by {@code @Async} methods
     * to apply {@link TenantContextTaskDecorator}, ensuring tenant context is automatically
     * propagated to asynchronous task threads.
     */
    @Bean
    public ThreadPoolTaskExecutorCustomizer tenantTaskExecutorCustomizer(TenantContextTaskDecorator taskDecorator) {
        return executor -> executor.setTaskDecorator(taskDecorator);
    }

    /**
     * Registers {@link TenantCacheKeyGenerator} as the default {@link KeyGenerator} for
     * Spring Cache, ensuring all {@code @Cacheable}, {@code @CachePut}, and {@code @CacheEvict}
     * operations produce tenant-isolated cache keys.
     * <p>
     * Only activated when Spring Cache is on the classpath ({@link KeyGenerator} class available).
     */
    @Bean
    @ConditionalOnClass(KeyGenerator.class)
    public KeyGenerator tenantCacheKeyGenerator() {
        return new TenantCacheKeyGenerator();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Nested configuration for Spring AMQP-dependent beans.
    // This class is only loaded by the JVM when Spring AMQP is on the classpath,
    // preventing NoClassDefFoundError in services without spring-boot-starter-amqp.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Nested auto-configuration that registers tenant-aware RabbitMQ infrastructure beans.
     * <p>
     * Isolated into a separate {@code @Configuration} class so that the JVM only attempts
     * to resolve AMQP-related class references when Spring AMQP is actually on the classpath.
     * This avoids {@code NoClassDefFoundError} in services (e.g., auth-service) that depend
     * on {@code common-lib} but do not include {@code spring-boot-starter-amqp}.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.amqp.core.MessagePostProcessor")
    static class TenantAmqpAutoConfiguration {

        /**
         * Registers {@code TenantMessagePostProcessor} that adds the {@code x-tenant-id} header
         * to all outbound RabbitMQ messages from the current tenant context.
         * <p>
         * Only activated when Spring AMQP is on the classpath.
         * Services without RabbitMQ dependency will not load this bean.
         */
        @Bean
        public com.outreach.platform.common.tenant.TenantMessagePostProcessor tenantMessagePostProcessor() {
            return new com.outreach.platform.common.tenant.TenantMessagePostProcessor();
        }

        /**
         * Registers {@code TenantMessageInterceptor} as an {@link org.aopalliance.aop.Advice} bean
         * that intercepts inbound RabbitMQ message handling to populate tenant context
         * from the {@code x-tenant-id} message header.
         * <p>
         * Only activated when Spring AMQP is on the classpath. The advice is automatically
         * applied to listener containers by declaring it as an {@code Advice} bean — Spring Boot's
         * {@code RabbitAnnotationDrivenConfiguration} picks up {@code Advice} beans and applies
         * them to the default container factory.
         */
        @Bean
        public com.outreach.platform.common.tenant.TenantMessageInterceptor tenantMessageInterceptor() {
            return new com.outreach.platform.common.tenant.TenantMessageInterceptor();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Nested configuration for Spring Data Redis-dependent beans.
    // This class is only loaded by the JVM when Spring Data Redis is on the classpath,
    // preventing NoClassDefFoundError in services without spring-boot-starter-data-redis.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Nested auto-configuration that registers tenant-aware Redis cache invalidation beans.
     * <p>
     * Isolated into a separate {@code @Configuration} class so that the JVM only attempts
     * to resolve Redis-related class references when Spring Data Redis is actually on the
     * classpath. This avoids {@code NoClassDefFoundError} in services that depend on
     * {@code common-lib} but do not include {@code spring-boot-starter-data-redis}.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class TenantRedisAutoConfiguration {

        /**
         * Registers {@code TenantCacheInvalidationService} for tracking and bulk-invalidating
         * tenant-scoped cache entries in Redis.
         * <p>
         * Only activated when {@code StringRedisTemplate} is available in the application context
         * (i.e., when {@code spring-boot-starter-data-redis} is on the classpath and Redis
         * auto-configuration has created the template bean).
         * <p>
         * This service enables O(M) cache invalidation on tenant deactivation (where M is the
         * number of that tenant's cache entries) by maintaining a Redis Set per tenant that
         * tracks all associated cache keys.
         */
        @Bean
        @ConditionalOnBean(name = "stringRedisTemplate")
        public com.outreach.platform.common.tenant.TenantCacheInvalidationService tenantCacheInvalidationService(
                org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
            return new com.outreach.platform.common.tenant.TenantCacheInvalidationService(redisTemplate);
        }
    }
}
