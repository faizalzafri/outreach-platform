package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Integration tests for email retry logic.
 * Verifies retry scheduling, max retry limit, and permanent failure transitions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class RetryServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // The app has @RabbitListener beans that start eagerly on context refresh — without a real
    // broker, they either fail to connect or (worse, if something else is listening on the
    // default localhost:5672) fail auth against it.
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private EmailDispatchService emailDispatchService;

    @Autowired
    private EmailRetryService retryService;

    @Autowired
    private EmailDeliveryRepository deliveryRepository;

    private MimeMessage mockMimeMessage;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    @Test
    void smtpFailure_recordsRetryAttempt() {
        doThrow(new RuntimeException("Connection timed out"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryDocument delivery = createDelivery("retry-event-1", "user@example.com", 0);
        EmailDeliveryDocument saved = deliveryRepository.save(delivery);

        emailDispatchService.dispatchEmail(saved);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            EmailDeliveryDocument updated = deliveryRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(updated.getAttempts()).isEqualTo(1);
            assertThat(updated.getNextRetryAt()).isNotNull();
            assertThat(updated.getErrorMessage()).contains("Connection timed out");
        });
    }

    @Test
    void maxRetriesExceeded_statusSetToPermanentlyFailed() {
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        // Simulate a delivery that has already failed (max-retry-attempts - 1) = 2 times in test profile
        EmailDeliveryDocument delivery = createDelivery("retry-event-2", "failing@example.com", 2);
        EmailDeliveryDocument saved = deliveryRepository.save(delivery);

        emailDispatchService.dispatchEmail(saved);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            EmailDeliveryDocument updated = deliveryRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.PERMANENTLY_FAILED);
            assertThat(updated.getAttempts()).isEqualTo(3);
            assertThat(updated.getNextRetryAt()).isNull();
        });
    }

    @Test
    void retryDueDeliveries_picksUpFailedDeliveriesPastRetryTime() {
        // Create a delivery that is due for retry (nextRetryAt in the past)
        EmailDeliveryDocument delivery = new EmailDeliveryDocument();
        delivery.setEventId("retry-event-3");
        delivery.setRecipientEmail("due@example.com");
        delivery.setRecipientName("Due User");
        delivery.setSubject("Retry Test");
        delivery.setBody("<p>Retry body</p>");
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setAttempts(1);
        delivery.setNextRetryAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        delivery.setCreatedAt(Instant.now());
        deliveryRepository.save(delivery);

        int retried = retryService.retryDueDeliveries();

        assertThat(retried).isEqualTo(1);

        // After retry, the delivery should be in PENDING status then dispatched
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            EmailDeliveryDocument updated = deliveryRepository
                    .findByEventId("retry-event-3").get(0);
            // After successful dispatch, status should be SENT
            assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.SENT);
        });
    }

    @Test
    void retryDueDeliveries_doesNotPickUpFutureRetries() {
        // Create a delivery with nextRetryAt in the future — should NOT be retried
        EmailDeliveryDocument delivery = new EmailDeliveryDocument();
        delivery.setEventId("retry-event-4");
        delivery.setRecipientEmail("future@example.com");
        delivery.setRecipientName("Future User");
        delivery.setSubject("Future Retry");
        delivery.setBody("<p>Not yet due</p>");
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setAttempts(1);
        delivery.setNextRetryAt(Instant.now().plus(1, ChronoUnit.HOURS));
        delivery.setCreatedAt(Instant.now());
        deliveryRepository.save(delivery);

        int retried = retryService.retryDueDeliveries();

        assertThat(retried).isEqualTo(0);

        // Status should remain FAILED
        EmailDeliveryDocument stillFailed = deliveryRepository.findByEventId("retry-event-4").get(0);
        assertThat(stillFailed.getStatus()).isEqualTo(DeliveryStatus.FAILED);
    }

    private EmailDeliveryDocument createDelivery(String eventId, String email, int attempts) {
        EmailDeliveryDocument delivery = new EmailDeliveryDocument();
        delivery.setEventId(eventId);
        delivery.setRecipientEmail(email);
        delivery.setRecipientName("Test User");
        delivery.setSubject("Test Subject");
        delivery.setBody("<p>Test body</p>");
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttempts(attempts);
        delivery.setCreatedAt(Instant.now());
        return delivery;
    }
}
