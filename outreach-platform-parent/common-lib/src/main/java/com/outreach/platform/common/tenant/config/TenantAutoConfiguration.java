package com.outreach.platform.common.tenant.config;

import com.outreach.platform.common.tenant.TenantCacheKeyGenerator;
import com.outreach.platform.common.tenant.TenantContextFilter;
import com.outreach.platform.common.tenant.TenantContextTaskDecorator;
import com.outreach.platform.common.tenant.TenantFilterAspect;
import com.outreach.platform.common.tenant.TenantMessageInterceptor;
import com.outreach.platform.common.tenant.TenantMessagePostProcessor;
import jakarta.persistence.EntityManager;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
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
 *   <li>{@link TenantMessagePostProcessor} — adds {@code x-tenant-id} header to outbound RabbitMQ messages (conditional on Spring AMQP)</li>
 *   <li>{@link TenantMessageInterceptor} — extracts {@code x-tenant-id} header from inbound RabbitMQ messages into tenant context (conditional on Spring AMQP)</li>
 * </ul>
 *
 * @see TenantContextFilter
 * @see TenantFilterAspect
 * @see TenantContextTaskDecorator
 */
@AutoConfiguration
@ConditionalOnProperty(name = "tenant.enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

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
     * Only activated when JPA is on the classpath (i.e., {@link EntityManager} is available).
     */
    @Bean
    @ConditionalOnClass(EntityManager.class)
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

    /**
     * Registers {@link TenantMessagePostProcessor} that adds the {@code x-tenant-id} header
     * to all outbound RabbitMQ messages from the current {@link com.outreach.platform.common.tenant.TenantContext}.
     * <p>
     * Only activated when Spring AMQP is on the classpath (i.e., {@link MessagePostProcessor} is available).
     * Services without RabbitMQ dependency will not load this bean.
     */
    @Bean
    @ConditionalOnClass(MessagePostProcessor.class)
    public TenantMessagePostProcessor tenantMessagePostProcessor() {
        return new TenantMessagePostProcessor();
    }

    /**
     * Registers {@link TenantMessageInterceptor} as an {@link Advice} bean that intercepts
     * inbound RabbitMQ message handling to populate {@link com.outreach.platform.common.tenant.TenantContext}
     * from the {@code x-tenant-id} message header.
     * <p>
     * Only activated when Spring AMQP is on the classpath (i.e., {@link SimpleRabbitListenerContainerFactory}
     * is available). The advice is automatically applied to listener containers by declaring it as an
     * {@link Advice} bean — Spring Boot's {@code RabbitAnnotationDrivenConfiguration} picks up
     * {@link Advice} beans and applies them to the default container factory.
     */
    @Bean
    @ConditionalOnClass(SimpleRabbitListenerContainerFactory.class)
    public Advice tenantMessageInterceptor() {
        return new TenantMessageInterceptor();
    }
}
