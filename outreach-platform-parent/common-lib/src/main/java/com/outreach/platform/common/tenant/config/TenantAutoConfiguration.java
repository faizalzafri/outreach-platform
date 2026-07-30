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

/** Auto-configuration that registers tenant infrastructure beans when tenant.enabled=true (default). */
@AutoConfiguration
@ConditionalOnProperty(name = "tenant.enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantAutoConfiguration.class);

    @PreDestroy
    public void onShutdown() {
        log.info("Graceful shutdown: draining active tenant contexts");
    }

    /** Registers TenantContextFilter as a high-precedence servlet filter. */
    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration() {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("tenantContextFilter");
        return registration;
    }

    /** Registers the Hibernate tenant filter aspect, conditional on JPA EntityManager. */
    @Bean
    @ConditionalOnBean(EntityManager.class)
    public TenantFilterAspect tenantFilterAspect(EntityManager entityManager) {
        return new TenantFilterAspect(entityManager);
    }

    /** Registers the task decorator for tenant context propagation to async threads. */
    @Bean
    public TenantContextTaskDecorator tenantContextTaskDecorator() {
        return new TenantContextTaskDecorator();
    }

    /** Applies TenantContextTaskDecorator to the default @Async ThreadPoolTaskExecutor. */
    @Bean
    public ThreadPoolTaskExecutorCustomizer tenantTaskExecutorCustomizer(TenantContextTaskDecorator taskDecorator) {
        return executor -> executor.setTaskDecorator(taskDecorator);
    }

    /** Registers tenant-aware cache key generator, conditional on Spring Cache classpath. */
    @Bean
    @ConditionalOnClass(KeyGenerator.class)
    public KeyGenerator tenantCacheKeyGenerator() {
        return new TenantCacheKeyGenerator();
    }

    // Nested config loaded only when Spring AMQP is on the classpath
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.amqp.core.MessagePostProcessor")
    static class TenantAmqpAutoConfiguration {

        /** Adds x-tenant-id header to outbound RabbitMQ messages. */
        @Bean
        public com.outreach.platform.common.tenant.TenantMessagePostProcessor tenantMessagePostProcessor() {
            return new com.outreach.platform.common.tenant.TenantMessagePostProcessor();
        }

        /** Extracts x-tenant-id header from inbound RabbitMQ messages into TenantContext. */
        @Bean
        public com.outreach.platform.common.tenant.TenantMessageInterceptor tenantMessageInterceptor() {
            return new com.outreach.platform.common.tenant.TenantMessageInterceptor();
        }
    }

    // Nested config loaded only when Spring Data Redis is on the classpath
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class TenantRedisAutoConfiguration {

        /** Registers TenantCacheInvalidationService for bulk tenant cache eviction. */
        @Bean
        @ConditionalOnBean(name = "stringRedisTemplate")
        public com.outreach.platform.common.tenant.TenantCacheInvalidationService tenantCacheInvalidationService(
                org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
            return new com.outreach.platform.common.tenant.TenantCacheInvalidationService(redisTemplate);
        }
    }
}
