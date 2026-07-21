package com.outreach.platform.common.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Shared Redis cache configuration providing sensible defaults for all platform services.
 *
 * <p>Activates only when Redis classes are on the classpath and no other CacheManager bean is defined.
 * Services with custom RedisCacheConfig (e.g., feedback-service, report-service) take precedence.</p>
 *
 * <p>Configuration properties under {@code platform.cache.ttl.*} allow per-cache TTL overrides,
 * for example: {@code platform.cache.ttl.eventCache=30m}</p>
 */
@Configuration
@EnableCaching
@ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
public class DefaultRedisCacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    /**
     * Provides a RedisCacheManager with Jackson JSON serialization and a 10-minute default TTL.
     * Only activates when no other CacheManager bean is already registered.
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     CacheTtlProperties cacheTtlProperties) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .transactionAware();

        // Apply per-cache TTL overrides from properties
        Map<String, Duration> ttlOverrides = cacheTtlProperties.getTtl();
        if (ttlOverrides != null && !ttlOverrides.isEmpty()) {
            for (Map.Entry<String, Duration> entry : ttlOverrides.entrySet()) {
                builder.withCacheConfiguration(entry.getKey(),
                        defaultConfig.entryTtl(entry.getValue()));
            }
        }

        return builder.build();
    }

    /**
     * Binds per-cache TTL overrides from {@code platform.cache.ttl.<cacheName>=<duration>} properties.
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheTtlProperties cacheTtlProperties() {
        return new CacheTtlProperties();
    }

    @ConfigurationProperties(prefix = "platform.cache")
    public static class CacheTtlProperties {

        private Map<String, Duration> ttl = Map.of();

        public Map<String, Duration> getTtl() {
            return ttl;
        }

        public void setTtl(Map<String, Duration> ttl) {
            this.ttl = ttl;
        }
    }
}
