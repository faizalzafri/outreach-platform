package com.outreach.platform.common.tenant.config;

import com.outreach.platform.common.tenant.TenantCacheKeyGenerator;
import com.outreach.platform.common.tenant.TenantContextTaskDecorator;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Auto-configuration that registers tenant infrastructure beans when tenant.enabled=true (default). */
@AutoConfiguration
@ConditionalOnProperty(name = "tenant.enabled", havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantAutoConfiguration.class);

    @PreDestroy
    public void onShutdown() {
        log.info("Graceful shutdown: draining active tenant contexts");
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

    // Nested config loaded only when the servlet API is on the classpath. Reactive services
    // (gateway-service) exclude spring-boot-starter-web entirely and have no equivalent of
    // jakarta.servlet.Filter — tenant extraction there happens via the gateway's own reactive
    // WebFilter chain instead. Same class-level-guard reasoning as TenantJpaAutoConfiguration below:
    // FilterRegistrationBean<TenantContextFilter>'s declared type requires jakarta.servlet.Filter to
    // be linkable regardless of any bean condition, so the guard must be on the class, not the method.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(jakarta.servlet.Filter.class)
    static class TenantServletAutoConfiguration {

        /** Registers TenantContextFilter as a high-precedence servlet filter. */
        @Bean
        public org.springframework.boot.web.servlet.FilterRegistrationBean<com.outreach.platform.common.tenant.TenantContextFilter>
                tenantContextFilterRegistration() {
            org.springframework.boot.web.servlet.FilterRegistrationBean<com.outreach.platform.common.tenant.TenantContextFilter> registration =
                    new org.springframework.boot.web.servlet.FilterRegistrationBean<>();
            registration.setFilter(new com.outreach.platform.common.tenant.TenantContextFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10);
            registration.setName("tenantContextFilter");
            return registration;
        }
    }

    // Nested config loaded only when JPA (jakarta.persistence.EntityManager) is on the classpath.
    // Must live in its own class-level-guarded nested @Configuration rather than a @Bean method on
    // the outer class: @ConditionalOnBean only skips *instantiation*, but Spring still reflectively
    // resolves every top-level @Bean method's signature to evaluate its conditions, which requires
    // EntityManager to be linkable regardless of the condition's outcome — a NoClassDefFoundError
    // for services (like ai-service) that exclude spring-boot-starter-data-jpa entirely. A
    // class-level @ConditionalOnClass is checked before this nested class is ever parsed, so no such
    // service ever needs to load it.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(jakarta.persistence.EntityManager.class)
    static class TenantJpaAutoConfiguration {

        /** Registers the Hibernate tenant filter aspect, conditional on a JPA EntityManager bean. */
        @Bean
        @ConditionalOnBean(jakarta.persistence.EntityManager.class)
        public com.outreach.platform.common.tenant.TenantFilterAspect tenantFilterAspect(
                jakarta.persistence.EntityManager entityManager) {
            return new com.outreach.platform.common.tenant.TenantFilterAspect(entityManager);
        }
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
