package com.outreach.platform.common.tenant.config;

import com.outreach.platform.common.tenant.TenantContextFilter;
import com.outreach.platform.common.tenant.TenantContextTaskDecorator;
import com.outreach.platform.common.tenant.TenantFilterAspect;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
}
