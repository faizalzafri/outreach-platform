package com.outreach.platform.report.service;

import com.outreach.platform.report.ReportServiceApplication;
import com.outreach.platform.report.model.CityScoreDto;
import com.outreach.platform.report.model.DashboardSummaryDto;
import com.outreach.platform.report.model.EventScoreDto;
import com.outreach.platform.report.model.NpsResultDto;
import com.outreach.platform.report.model.ReportQueryParams;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Explicit classes=: bare @SpringBootTest auto-detects the nearest @SpringBootConfiguration by
// walking up from this test's own package, and RabbitMqEventListenerIT's nested TestApp (a
// minimal @SpringBootApplication for that class's own narrow test) lives in this exact same
// package — closer than the real ReportServiceApplication one package up — so it was being
// picked instead, booting the wrong context entirely.
//
// Deliberately NOT @TestInstance(PER_CLASS): with PER_CLASS, JUnit creates the single test
// instance (running TestInstancePostProcessors, including SpringExtension's context-loading
// one) before @Testcontainers' BeforeAllCallback starts the static containers — so the
// @DynamicPropertySource supplier calls postgres::getJdbcUrl while the container isn't running
// yet ("Mapped port can only be obtained after the container is started"). Default PER_METHOD
// avoids the ordering issue; schemaCreated below still only creates the schema once per method
// via CREATE TABLE IF NOT EXISTS, so re-running it per test instance is harmless.
@SpringBootTest(classes = ReportServiceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class ReportServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("reportdb")
            .withUsername("test")
            .withPassword("test");

    // The app has @RabbitListener beans that start eagerly on context refresh — without a real
    // broker, they either fail to connect or (worse, if something else is listening on the
    // default localhost:5672) fail auth against it.
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    // Excluding Mongo autoconfig (the previous approach) left ExportJobRepository — a real Mongo
    // repository the app unconditionally scans via @EnableMongoRepositories — with no
    // mongoTemplate bean, so the context failed to start once this test began booting the real
    // ReportServiceApplication instead of the wrong sibling-class config. A real container is
    // needed instead, same lesson as MongoDbIndexIT in event-service.
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");

    // ReportService's methods are all @Cacheable, which requires a real RedisConnectionFactory
    // for RedisCacheConfig's cacheManager bean.
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // application-test.yml excludes RedisAutoConfiguration globally (most tests don't need
        // real caching) — override back to nothing so RedisCacheConfig's cacheManager bean gets
        // a real RedisConnectionFactory from the container above.
        registry.add("spring.autoconfigure.exclude", () -> "");
    }

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private ReportService reportService;

    private boolean schemaCreated = false;

    @BeforeEach
    void seedData() {
        if (!schemaCreated) {
            createSchema();
            schemaCreated = true;
        }
        jdbcTemplate.execute("DELETE FROM volunteer_feedback");
        jdbcTemplate.execute("DELETE FROM volunteers");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM beneficiaries");

        // Events
        jdbcTemplate.execute("""
                INSERT INTO events (id, event_name, city, status, event_date) VALUES
                (1, 'City Cleanup', 'Mumbai', 'COMPLETED', '2024-03-15'),
                (2, 'Tree Plantation', 'Delhi', 'COMPLETED', '2024-04-10'),
                (3, 'Food Drive', 'Mumbai', 'PLANNED', '2024-05-01')
                """);

        // Volunteers
        jdbcTemplate.execute("""
                INSERT INTO volunteers (id, name, total_events_participated) VALUES
                (1, 'Alice', 3),
                (2, 'Bob', 2),
                (3, 'Charlie', 1)
                """);

        // Beneficiaries
        jdbcTemplate.execute("""
                INSERT INTO beneficiaries (id, name, active) VALUES
                (1, 'NGO Alpha', true),
                (2, 'NGO Beta', false)
                """);

        // Feedback: scores are on 1-5 scale
        // Event 1 (City Cleanup): scores 5, 4, 3 -> avg = 4.00
        // Event 2 (Tree Plantation): scores 2, 1 -> avg = 1.50
        jdbcTemplate.execute("""
                INSERT INTO volunteer_feedback (id, event_id, volunteer_id, score, sentiment, submitted_at) VALUES
                (1, 1, 1, 5, 'POSITIVE', '2024-03-16 10:00:00'),
                (2, 1, 2, 4, 'POSITIVE', '2024-03-16 11:00:00'),
                (3, 1, 3, 3, 'NEUTRAL', '2024-03-16 12:00:00'),
                (4, 2, 1, 2, 'NEGATIVE', '2024-04-11 10:00:00'),
                (5, 2, 2, 1, 'NEGATIVE', '2024-04-11 11:00:00')
                """);
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    id BIGSERIAL PRIMARY KEY,
                    event_name VARCHAR(255) NOT NULL,
                    city VARCHAR(100),
                    status VARCHAR(50) DEFAULT 'PLANNED',
                    event_date DATE
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS volunteers (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255),
                    total_events_participated INT DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS volunteer_feedback (
                    id BIGSERIAL PRIMARY KEY,
                    event_id BIGINT NOT NULL REFERENCES events(id),
                    volunteer_id BIGINT NOT NULL REFERENCES volunteers(id),
                    score INT NOT NULL,
                    sentiment VARCHAR(20),
                    submitted_at TIMESTAMP DEFAULT NOW()
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS beneficiaries (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255),
                    active BOOLEAN DEFAULT true
                )
                """);
    }

    @Test
    void aggregateByEvent_returnsCorrectAverages() {
        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);

        List<EventScoreDto> results = reportService.aggregateByEvent(params);

        assertThat(results).hasSize(2);

        // Results are ordered by avg_score DESC, so City Cleanup (4.00) first
        EventScoreDto cityCleanup = results.get(0);
        assertThat(cityCleanup.eventName()).isEqualTo("City Cleanup");
        assertThat(cityCleanup.city()).isEqualTo("Mumbai");
        assertThat(cityCleanup.averageScore()).isEqualTo(new BigDecimal("4.00"));
        assertThat(cityCleanup.feedbackCount()).isEqualTo(3);
        assertThat(cityCleanup.minScore()).isEqualTo(3);
        assertThat(cityCleanup.maxScore()).isEqualTo(5);

        EventScoreDto treePlantation = results.get(1);
        assertThat(treePlantation.eventName()).isEqualTo("Tree Plantation");
        assertThat(treePlantation.averageScore()).isEqualTo(new BigDecimal("1.50"));
        assertThat(treePlantation.feedbackCount()).isEqualTo(2);
    }

    @Test
    void aggregateByCity_groupsCorrectly() {
        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);

        List<CityScoreDto> results = reportService.aggregateByCity(params);

        assertThat(results).hasSize(2);

        // Mumbai has higher avg (4.00) so it comes first
        CityScoreDto mumbai = results.get(0);
        assertThat(mumbai.city()).isEqualTo("Mumbai");
        assertThat(mumbai.averageScore()).isEqualTo(new BigDecimal("4.00"));
        assertThat(mumbai.feedbackCount()).isEqualTo(3);
        assertThat(mumbai.eventCount()).isEqualTo(1); // Only event 1 has feedback in Mumbai
        assertThat(mumbai.volunteerCount()).isEqualTo(3);

        CityScoreDto delhi = results.get(1);
        assertThat(delhi.city()).isEqualTo("Delhi");
        assertThat(delhi.averageScore()).isEqualTo(new BigDecimal("1.50"));
        assertThat(delhi.feedbackCount()).isEqualTo(2);
        assertThat(delhi.eventCount()).isEqualTo(1);
        assertThat(delhi.volunteerCount()).isEqualTo(2);
    }

    @Test
    void getDashboardSummary_returnsExpectedCounts() {
        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);

        DashboardSummaryDto summary = reportService.getDashboardSummary(params);

        assertThat(summary.totalEvents()).isEqualTo(3);
        assertThat(summary.completedEvents()).isEqualTo(2);
        assertThat(summary.totalVolunteers()).isEqualTo(3);
        assertThat(summary.totalFeedbackSubmissions()).isEqualTo(5);
        // Overall average: (5+4+3+2+1)/5 = 3.00
        assertThat(summary.overallAverageScore()).isEqualTo(new BigDecimal("3.00"));
        assertThat(summary.totalBeneficiaries()).isEqualTo(1); // Only active ones
        assertThat(summary.activeCities()).isEqualTo(2); // Mumbai, Delhi
    }

    @Test
    void getNps_calculatesPromotersAndDetractorsCorrectly() {
        ReportQueryParams params = new ReportQueryParams(null, null, null, null, null, null, null);

        List<NpsResultDto> results = reportService.getNps(params);

        assertThat(results).hasSize(2);

        // Ordered by event_name: City Cleanup, Tree Plantation
        NpsResultDto cityCleanup = results.get(0);
        assertThat(cityCleanup.eventName()).isEqualTo("City Cleanup");
        // Scores: 5 (promoter), 4 (promoter), 3 (passive) -> promoters=2, passives=1, detractors=0
        assertThat(cityCleanup.promoters()).isEqualTo(2);
        assertThat(cityCleanup.passives()).isEqualTo(1);
        assertThat(cityCleanup.detractors()).isEqualTo(0);
        assertThat(cityCleanup.totalResponses()).isEqualTo(3);
        // NPS = (2 - 0) * 100 / 3 = 66.7
        BigDecimal expectedNps = BigDecimal.valueOf(200.0 / 3).setScale(1, RoundingMode.HALF_UP);
        assertThat(cityCleanup.npsScore()).isEqualTo(expectedNps);

        NpsResultDto treePlantation = results.get(1);
        assertThat(treePlantation.eventName()).isEqualTo("Tree Plantation");
        // Scores: 2 (detractor), 1 (detractor) -> promoters=0, passives=0, detractors=2
        assertThat(treePlantation.promoters()).isEqualTo(0);
        assertThat(treePlantation.passives()).isEqualTo(0);
        assertThat(treePlantation.detractors()).isEqualTo(2);
        assertThat(treePlantation.totalResponses()).isEqualTo(2);
        // NPS = (0 - 2) * 100 / 2 = -100.0
        assertThat(treePlantation.npsScore()).isEqualTo(new BigDecimal("-100.0"));
    }
}
