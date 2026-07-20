package com.outreach.platform.event.config;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MongoDB connection pool configuration.
 *
 * <p>Configures the MongoDB driver connection pool with settings suitable for
 * a production microservice: minimum idle connections to avoid cold-start latency,
 * bounded maximum connections to prevent resource exhaustion, and reasonable timeouts.</p>
 *
 * <p>Settings are externalized under {@code spring.data.mongodb} in application.yml:</p>
 * <pre>
 * spring:
 *   data:
 *     mongodb:
 *       min-connections-per-host: 5
 *       max-connections-per-host: 20
 *       connect-timeout: 5000
 *       socket-timeout: 10000
 * </pre>
 *
 * <p>This configuration is reusable across all services that connect to MongoDB.
 * Other services can copy this class and adjust defaults as needed.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoConnectionPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionPoolConfig.class);

    private int minConnectionsPerHost = 5;
    private int maxConnectionsPerHost = 20;
    private int connectTimeout = 5000;
    private int socketTimeout = 10000;

    /**
     * Customizes the MongoDB client connection pool and socket settings.
     *
     * <p>Connection pool: min 5, max 20 connections.
     * Socket: connect timeout 5s, read timeout 10s.</p>
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoConnectionPoolCustomizer() {
        return builder -> {
            builder.applyToConnectionPoolSettings(pool -> {
                pool.minSize(minConnectionsPerHost);
                pool.maxSize(maxConnectionsPerHost);
                pool.maxWaitTime(connectTimeout, TimeUnit.MILLISECONDS);
            });
            builder.applyToSocketSettings(socket -> {
                socket.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
                socket.readTimeout(socketTimeout, TimeUnit.MILLISECONDS);
            });

            log.info("MongoDB connection pool configured: minSize={}, maxSize={}, connectTimeout={}ms",
                    minConnectionsPerHost, maxConnectionsPerHost, connectTimeout);
        };
    }

    // ─── Getters and Setters (for @ConfigurationProperties binding) ─────────

    public int getMinConnectionsPerHost() {
        return minConnectionsPerHost;
    }

    public void setMinConnectionsPerHost(int minConnectionsPerHost) {
        this.minConnectionsPerHost = minConnectionsPerHost;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}
