package com.outreach.platform.event;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.outreach.platform.event.config.MongoDbInitializer;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies MongoDB collections and indexes are created
 * correctly by the MongoDbInitializer on application startup.
 *
 * Validates that MongoDB collections and indexes are created correctly on startup.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("MongoDB Index Integration Tests")
class MongoDbIndexIT {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");

    // The app context can't start without a datasource — AdminService/other beans require JPA
    // repositories unconditionally — so a real Postgres container is needed here too, even though
    // this suite only asserts against Mongo collections/indexes (previously tried to exclude JPA
    // entirely, which left UserRepository with no bean and failed context startup for every test).
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

    @Autowired
    private MongoClient mongoClient;

    private MongoDatabase database;

    @BeforeEach
    void setUp() {
        database = mongoClient.getDatabase(mongoTemplate.getDb().getName());
    }

    // ===== Collection Verification =====

    @Test
    @DisplayName("All 6 required MongoDB collections are created")
    void shouldCreateAllRequiredCollections() {
        Set<String> existingCollections = mongoTemplate.getCollectionNames();

        assertThat(existingCollections).containsAll(MongoDbInitializer.REQUIRED_COLLECTIONS);
    }

    @Test
    @DisplayName("domain_events collection exists with proper indexes")
    void shouldHaveDomainEventsIndexes() {
        List<Document> indexes = getIndexes("domain_events");
        List<String> indexNames = getIndexNames(indexes);

        assertThat(indexNames).contains(
                "idx_domain_events_status_createdAt",
                "idx_domain_events_eventType",
                "idx_domain_events_ttl_30d"
        );
    }

    @Test
    @DisplayName("audit_logs collection exists with proper indexes")
    void shouldHaveAuditLogsIndexes() {
        List<Document> indexes = getIndexes("audit_logs");
        List<String> indexNames = getIndexNames(indexes);

        assertThat(indexNames).contains(
                "idx_audit_logs_userId_timestamp",
                "idx_audit_logs_action",
                "idx_audit_logs_resource",
                "idx_audit_logs_ttl_90d"
        );
    }

    @Test
    @DisplayName("job_tracking collection exists with proper indexes")
    void shouldHaveJobTrackingIndexes() {
        List<Document> indexes = getIndexes("job_tracking");
        List<String> indexNames = getIndexNames(indexes);

        assertThat(indexNames).contains(
                "idx_job_tracking_status_createdAt",
                "idx_job_tracking_jobType",
                "idx_job_tracking_ttl_60d"
        );
    }

    @Test
    @DisplayName("email_deliveries collection exists with proper indexes")
    void shouldHaveEmailDeliveriesIndexes() {
        List<Document> indexes = getIndexes("email_deliveries");
        List<String> indexNames = getIndexNames(indexes);

        assertThat(indexNames).contains(
                "idx_email_deliveries_eventId_status",
                "idx_email_deliveries_recipientEmail",
                "idx_email_deliveries_status_lastAttemptAt",
                "idx_email_deliveries_ttl_90d"
        );
    }

    @Test
    @DisplayName("analytics_snapshots collection exists with proper indexes")
    void shouldHaveAnalyticsSnapshotsIndexes() {
        List<Document> indexes = getIndexes("analytics_snapshots");
        List<String> indexNames = getIndexNames(indexes);

        assertThat(indexNames).contains(
                "idx_analytics_snapshots_type_createdAt",
                "idx_analytics_snapshots_eventId",
                "idx_analytics_snapshots_ttl_365d"
        );
    }

    @Test
    @DisplayName("file_metadata collection exists with proper indexes")
    void shouldHaveFileMetadataIndexes() {
        List<Document> indexes = getIndexes("file_metadata");
        List<String> indexNames = getIndexNames(indexes);

        assertThat(indexNames).contains(
                "idx_file_metadata_jobId",
                "idx_file_metadata_status",
                "idx_file_metadata_ttl_90d"
        );
    }

    // ===== TTL Index Verification =====

    @Test
    @DisplayName("domain_events TTL index expires after 30 days")
    void shouldHaveDomainEventsTtlExpiration() {
        List<Document> indexes = getIndexes("domain_events");
        Document ttlIndex = findIndexByName(indexes, "idx_domain_events_ttl_30d");

        assertThat(ttlIndex).isNotNull();
        // TTL is in seconds: 30 days = 2592000 seconds
        Number expireAfterSeconds = (Number) ttlIndex.get("expireAfterSeconds");
        assertThat(expireAfterSeconds.longValue()).isEqualTo(30L * 24 * 60 * 60);
    }

    @Test
    @DisplayName("audit_logs TTL index expires after 365 days")
    void shouldHaveAuditLogsTtlExpiration() {
        List<Document> indexes = getIndexes("audit_logs");
        Document ttlIndex = findIndexByName(indexes, "idx_audit_logs_ttl_90d");

        assertThat(ttlIndex).isNotNull();
        // TTL: 90 days = 7776000 seconds
        Number expireAfterSeconds = (Number) ttlIndex.get("expireAfterSeconds");
        assertThat(expireAfterSeconds.longValue()).isEqualTo(90L * 24 * 60 * 60);
    }

    @Test
    @DisplayName("job_tracking TTL index expires after 90 days")
    void shouldHaveJobTrackingTtlExpiration() {
        List<Document> indexes = getIndexes("job_tracking");
        Document ttlIndex = findIndexByName(indexes, "idx_job_tracking_ttl_60d");

        assertThat(ttlIndex).isNotNull();
        Number expireAfterSeconds = (Number) ttlIndex.get("expireAfterSeconds");
        assertThat(expireAfterSeconds.longValue()).isEqualTo(60L * 24 * 60 * 60);
    }

    @Test
    @DisplayName("email_deliveries TTL index expires after 90 days")
    void shouldHaveEmailDeliveriesTtlExpiration() {
        List<Document> indexes = getIndexes("email_deliveries");
        Document ttlIndex = findIndexByName(indexes, "idx_email_deliveries_ttl_90d");

        assertThat(ttlIndex).isNotNull();
        Number expireAfterSeconds = (Number) ttlIndex.get("expireAfterSeconds");
        assertThat(expireAfterSeconds.longValue()).isEqualTo(90L * 24 * 60 * 60);
    }

    @Test
    @DisplayName("analytics_snapshots TTL index expires after 365 days")
    void shouldHaveAnalyticsSnapshotsTtlExpiration() {
        List<Document> indexes = getIndexes("analytics_snapshots");
        Document ttlIndex = findIndexByName(indexes, "idx_analytics_snapshots_ttl_365d");

        assertThat(ttlIndex).isNotNull();
        Number expireAfterSeconds = (Number) ttlIndex.get("expireAfterSeconds");
        assertThat(expireAfterSeconds.longValue()).isEqualTo(365L * 24 * 60 * 60);
    }

    // ===== Helper Methods =====

    private List<Document> getIndexes(String collectionName) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        ListIndexesIterable<Document> indexesIterable = collection.listIndexes();
        List<Document> indexes = new ArrayList<>();
        indexesIterable.into(indexes);
        return indexes;
    }

    private List<String> getIndexNames(List<Document> indexes) {
        return indexes.stream()
                .map(doc -> doc.getString("name"))
                .collect(Collectors.toList());
    }

    private Document findIndexByName(List<Document> indexes, String name) {
        return indexes.stream()
                .filter(doc -> name.equals(doc.getString("name")))
                .findFirst()
                .orElse(null);
    }
}
