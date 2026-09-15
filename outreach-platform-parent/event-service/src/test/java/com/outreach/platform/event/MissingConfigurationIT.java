package com.outreach.platform.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test that verifies the application fails to start with descriptive
 * errors when required database configuration is missing or unreachable.
 *
 * Validates that missing or unreachable database configuration causes descriptive startup failure.
 */
@Tag("integration")
@Testcontainers
@DisplayName("Missing Configuration Startup Failure Tests")
class MissingConfigurationIT {

    // Only used by shouldFailStartupWhenMongoDbUriIsInvalid — that test needs a real, working
    // Postgres so JPA beans like UserRepository (required unconditionally by AdminService) start
    // up fine, isolating the induced failure to the deliberately-unreachable Mongo URI. Excluding
    // DataSourceAutoConfiguration/HibernateJpaAutoConfiguration entirely (the previous approach)
    // left UserRepository with no bean at all, failing before Mongo was ever reached.
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    @DisplayName("Application fails to start when PostgreSQL is unreachable")
    void shouldFailStartupWhenPostgresqlIsUnreachable() {
        assertThatThrownBy(() -> {
            SpringApplication app = new SpringApplication(EventServiceApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", "jdbc:postgresql://unreachable-host:5432/nonexistent");
            props.put("spring.datasource.username", "postgres");
            props.put("spring.datasource.password", "postgres");
            props.put("spring.datasource.hikari.connection-timeout", "2000");
            props.put("spring.datasource.hikari.initialization-fail-timeout", "2000");
            props.put("spring.liquibase.enabled", "true");
            props.put("spring.data.mongodb.uri", "mongodb://localhost:27017/test_unused");
            props.put("spring.jpa.hibernate.ddl-auto", "validate");
            props.put("spring.cloud.discovery.enabled", "false");
            props.put("eureka.client.enabled", "false");
            props.put("spring.autoconfigure.exclude",
                    "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration");
            app.setDefaultProperties(props);
            ConfigurableApplicationContext ctx = app.run();
            ctx.close();
        }).satisfies(ex -> {
            // The exception message should be descriptive about the connection failure
            String fullError = getFullExceptionChain(ex);
            assertThatContainsConnectionError(fullError);
        });
    }

    @Test
    @DisplayName("Application fails to start when MongoDB URI is invalid")
    void shouldFailStartupWhenMongoDbUriIsInvalid() {
        assertThatThrownBy(() -> {
            SpringApplication app = new SpringApplication(EventServiceApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            // Command-line args, not setDefaultProperties: application.yml's own
            // spring.datasource.username/password (${DB_USERNAME:postgres} etc.) resolve to a
            // concrete value and outrank the SpringApplication default-properties source, so a
            // defaultProperties override here is silently ignored — this test is the only one of
            // the three that actually needs its datasource override to win (the other two only
            // need the connection to fail, which happens before auth either way).
            ConfigurableApplicationContext ctx = app.run(
                    "--spring.datasource.url=" + postgres.getJdbcUrl(),
                    "--spring.datasource.username=" + postgres.getUsername(),
                    "--spring.datasource.password=" + postgres.getPassword(),
                    "--spring.data.mongodb.uri=mongodb://unreachable-mongo-host:27017/test",
                    "--spring.data.mongodb.auto-index-creation=true",
                    "--spring.liquibase.enabled=true",
                    "--spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
                    "--spring.jpa.hibernate.ddl-auto=validate",
                    "--spring.cloud.discovery.enabled=false",
                    "--eureka.client.enabled=false",
                    "--spring.autoconfigure.exclude="
                            + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration");
            // Force MongoDB connection to be established
            ctx.getBean(org.springframework.data.mongodb.core.MongoTemplate.class)
                    .getCollectionNames();
            ctx.close();
        }).satisfies(ex -> {
            // Should fail due to MongoDB connection issues
            String fullMessage = getFullExceptionChain(ex);
            assertThatContainsMongoOrConnectionError(fullMessage);
        });
    }

    @Test
    @DisplayName("Liquibase migration failure halts startup with descriptive error")
    void shouldFailStartupWhenLiquibaseMigrationFails() {
        assertThatThrownBy(() -> {
            SpringApplication app = new SpringApplication(EventServiceApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", "jdbc:postgresql://unreachable-host:5432/nonexistent");
            props.put("spring.datasource.username", "postgres");
            props.put("spring.datasource.password", "postgres");
            props.put("spring.datasource.hikari.connection-timeout", "2000");
            props.put("spring.datasource.hikari.initialization-fail-timeout", "2000");
            props.put("spring.liquibase.enabled", "true");
            props.put("spring.liquibase.change-log", "classpath:db/changelog/db.changelog-master.xml");
            props.put("spring.data.mongodb.uri", "mongodb://localhost:27017/test_unused");
            props.put("spring.jpa.hibernate.ddl-auto", "none");
            props.put("spring.cloud.discovery.enabled", "false");
            props.put("eureka.client.enabled", "false");
            props.put("spring.autoconfigure.exclude",
                    "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                            + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration");
            app.setDefaultProperties(props);
            ConfigurableApplicationContext ctx = app.run();
            ctx.close();
        }).satisfies(ex -> {
            // Startup should fail - Liquibase or datasource connection error
            String fullMessage = getFullExceptionChain(ex);
            assertThat(fullMessage).isNotEmpty();
        });
    }

    // ===== Helper Methods =====

    private void assertThatContainsConnectionError(String errorMessage) {
        // Descriptive connection errors contain references to the host or connection failure
        assertThat(errorMessage.toLowerCase())
                .satisfiesAnyOf(
                        msg -> assertThat(msg).contains("unreachable"),
                        msg -> assertThat(msg).contains("connection"),
                        msg -> assertThat(msg).contains("refused"),
                        msg -> assertThat(msg).contains("timeout"),
                        msg -> assertThat(msg).contains("unable to obtain"),
                        msg -> assertThat(msg).contains("unknown host"),
                        msg -> assertThat(msg).contains("communicationsexception"),
                        msg -> assertThat(msg).contains("does not exist"),
                        msg -> assertThat(msg).contains("psqlexception"),
                        msg -> assertThat(msg).contains("fatal")
                );
    }

    private void assertThatContainsMongoOrConnectionError(String errorMessage) {
        assertThat(errorMessage.toLowerCase())
                .satisfiesAnyOf(
                        msg -> assertThat(msg).contains("unreachable"),
                        msg -> assertThat(msg).contains("connection"),
                        msg -> assertThat(msg).contains("refused"),
                        msg -> assertThat(msg).contains("timeout"),
                        msg -> assertThat(msg).contains("mongo"),
                        msg -> assertThat(msg).contains("unknown host"),
                        msg -> assertThat(msg).contains("server selection")
                );
    }

    private String getFullExceptionChain(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append(" ");
            }
            current = current.getCause();
        }
        return sb.toString();
    }
}
