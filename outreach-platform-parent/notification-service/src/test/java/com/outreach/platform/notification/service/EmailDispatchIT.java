package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Integration tests for email dispatch functionality.
 * Verifies that emails are sent correctly and delivery status is tracked in MongoDB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class EmailDispatchIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

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
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private EmailDispatchService emailDispatchService;

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
    void dispatchEmail_sendsEmailAndRecordsDeliveryStatus() {
        EmailDeliveryDocument delivery = createDelivery(
                "event-123",
                "volunteer@example.com",
                "Jane Doe",
                "Feedback Request",
                "<p>Please provide your feedback.</p>"
        );
        EmailDeliveryDocument saved = deliveryRepository.save(delivery);

        emailDispatchService.dispatchEmail(saved);

        // Wait for async execution to complete
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            EmailDeliveryDocument updated = deliveryRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.SENT);
            assertThat(updated.getSentAt()).isNotNull();
            assertThat(updated.getAttempts()).isEqualTo(1);
            assertThat(updated.getErrorMessage()).isNull();
        });

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void dispatchEmail_correctRecipientAndSubject() {
        EmailDeliveryDocument delivery = createDelivery(
                "event-456",
                "alice@example.com",
                "Alice Smith",
                "Welcome to Outreach!",
                "<h1>Welcome Alice!</h1>"
        );
        EmailDeliveryDocument saved = deliveryRepository.save(delivery);

        emailDispatchService.dispatchEmail(saved);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            EmailDeliveryDocument updated = deliveryRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.SENT);
            assertThat(updated.getRecipientEmail()).isEqualTo("alice@example.com");
            assertThat(updated.getSubject()).isEqualTo("Welcome to Outreach!");
        });
    }

    @Test
    void dispatchEmail_onSmtpFailure_recordsFailedStatusWithRetry() {
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryDocument delivery = createDelivery(
                "event-789",
                "bob@example.com",
                "Bob Jones",
                "Event Update",
                "<p>Your event has been updated.</p>"
        );
        EmailDeliveryDocument saved = deliveryRepository.save(delivery);

        emailDispatchService.dispatchEmail(saved);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            EmailDeliveryDocument updated = deliveryRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(updated.getAttempts()).isEqualTo(1);
            assertThat(updated.getErrorMessage()).contains("SMTP connection refused");
            assertThat(updated.getNextRetryAt()).isNotNull();
            assertThat(updated.getNextRetryAt()).isAfter(Instant.now());
        });
    }

    @Test
    void dispatchEmail_deliveryRecordedInMongoDB() {
        EmailDeliveryDocument delivery = createDelivery(
                "event-track-001",
                "tracking@example.com",
                "Track User",
                "Tracking Test",
                "<p>Tracking delivery record.</p>"
        );
        EmailDeliveryDocument saved = deliveryRepository.save(delivery);

        emailDispatchService.dispatchEmail(saved);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var allByEvent = deliveryRepository.findByEventId("event-track-001");
            assertThat(allByEvent).hasSize(1);
            assertThat(allByEvent.get(0).getRecipientEmail()).isEqualTo("tracking@example.com");
            assertThat(allByEvent.get(0).getStatus()).isEqualTo(DeliveryStatus.SENT);
        });
    }

    private EmailDeliveryDocument createDelivery(String eventId, String email,
                                                  String name, String subject, String body) {
        EmailDeliveryDocument delivery = new EmailDeliveryDocument();
        delivery.setEventId(eventId);
        delivery.setRecipientEmail(email);
        delivery.setRecipientName(name);
        delivery.setSubject(subject);
        delivery.setBody(body);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setCreatedAt(Instant.now());
        return delivery;
    }
}
