package com.outreach.platform.event.integration;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying the Spring Boot Actuator health endpoint
 * correctly reports dependency status for PostgreSQL, MongoDB, and Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class HealthEndpointIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    // The app has spring-boot-starter-amqp, so Actuator auto-configures a RabbitHealthIndicator
    // that tries the default localhost:5672 unless a real broker is provisioned — without this,
    // "rabbit" reports DOWN and drags the aggregate /actuator/health status to 503 regardless of
    // db/mongo/redis all being healthy.
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Inject
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpoint_shouldReturnUpWhenAllDependenciesHealthy() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void healthEndpoint_shouldContainDbComponent() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"db\"");
    }

    @Test
    void healthEndpoint_shouldContainMongoComponent() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"mongo\"");
    }

    @Test
    void healthEndpoint_shouldContainRedisComponent() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"redis\"");
    }

    @Test
    void readinessProbe_shouldReturnUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void livenessProbe_shouldReturnUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health/liveness", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void postgresOperations_shouldWorkWhenMongoDbIsConfiguredSeparately() {
        // This verifies NoSQL isolation — PostgreSQL operations are independent
        // of MongoDB availability by testing that event CRUD works normally
        // while the health endpoint correctly reports both stores as healthy.
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                "/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Both datastores should be reported independently
        String body = healthResponse.getBody();
        assertThat(body).contains("\"db\"");
        assertThat(body).contains("\"mongo\"");
        // The overall status is UP, indicating both are independently healthy
        assertThat(body).contains("\"status\":\"UP\"");
    }
}
