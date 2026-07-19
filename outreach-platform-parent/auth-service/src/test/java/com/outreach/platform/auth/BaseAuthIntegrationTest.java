package com.outreach.platform.auth;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for auth-service integration tests.
 * Provides a shared Testcontainers PostgreSQL 16 instance, disables Eureka,
 * and activates the embedded Spring Authorization Server ({@code idp.provider=spring}).
 *
 * <p>The PostgreSQL container is started once in a static initializer and shared across
 * ALL test classes. This avoids Spring context caching issues where HikariPool retains
 * a connection to a dead container when each test class creates its own container instance.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseAuthIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable Eureka for tests
        registry.add("eureka.client.enabled", () -> "false");
        // Activate embedded Spring Authorization Server
        registry.add("idp.provider", () -> "spring");
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
