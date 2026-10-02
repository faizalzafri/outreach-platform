package com.outreach.platform.event;

import com.outreach.platform.event.model.AuditLogDocument;
import com.outreach.platform.event.model.DomainEventDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The indexes and retention (TTL) declared on event-service's documents exist once the app starts.
 * The names match the ones databases already have, so existing deployments keep their indexes.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("MongoDB Index Integration Tests")
class MongoDbIndexIT {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");

    // The app context needs a datasource for its JPA repositories even though this suite only
    // looks at Mongo.
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @DisplayName("domain_events: lookup indexes, and published events expire after 30 days")
    void domainEvents() {
        Map<String, IndexInfo> indexes = indexes(DomainEventDocument.class);

        assertThat(indexes).containsKeys("idx_domain_events_status_createdAt", "idx_domain_events_eventType");
        assertThat(indexes.get("idx_domain_events_ttl_30d").getExpireAfter()).contains(Duration.ofDays(30));
    }

    @Test
    @DisplayName("audit_logs: lookup indexes, and entries expire after 90 days")
    void auditLogs() {
        Map<String, IndexInfo> indexes = indexes(AuditLogDocument.class);

        assertThat(indexes).containsKeys(
                "idx_audit_logs_userId_timestamp", "idx_audit_logs_action", "idx_audit_logs_resource");
        assertThat(indexes.get("idx_audit_logs_ttl_90d").getExpireAfter()).contains(Duration.ofDays(90));
    }

    private Map<String, IndexInfo> indexes(Class<?> document) {
        return mongoTemplate.indexOps(document).getIndexInfo().stream()
                .collect(Collectors.toMap(IndexInfo::getName, i -> i));
    }
}
