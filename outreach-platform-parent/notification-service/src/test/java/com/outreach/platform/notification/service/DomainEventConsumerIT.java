package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.DomainEventDocument;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.DomainEventRepository;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests verifying that domain event consumption triggers email dispatch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class DomainEventConsumerIT {

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
    private DomainEventConsumer domainEventConsumer;

    @Autowired
    private DomainEventRepository domainEventRepository;

    @Autowired
    private EmailDeliveryRepository emailDeliveryRepository;

    @BeforeEach
    void setUp() {
        domainEventRepository.deleteAll();
        emailDeliveryRepository.deleteAll();
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    @Test
    void sendFeedbackEmailsEvent_createsDeliveryRecordsAndDispatches() {
        DomainEventDocument event = new DomainEventDocument();
        event.setEventType("SendFeedbackEmails");
        event.setProcessed(false);
        event.setCreatedAt(Instant.now());
        event.setPayload(Map.of(
                "eventId", "outreach-event-001",
                "recipients", List.of(
                        Map.of("email", "user1@example.com", "name", "User One"),
                        Map.of("email", "user2@example.com", "name", "User Two")
                )
        ));
        domainEventRepository.save(event);

        // Trigger event polling manually
        domainEventConsumer.pollDomainEvents();

        // Verify delivery records created and dispatched
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EmailDeliveryDocument> deliveries =
                    emailDeliveryRepository.findByEventId("outreach-event-001");
            assertThat(deliveries).hasSize(2);
            assertThat(deliveries)
                    .extracting(EmailDeliveryDocument::getRecipientEmail)
                    .containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
            assertThat(deliveries)
                    .allMatch(d -> d.getStatus() == DeliveryStatus.SENT
                            || d.getStatus() == DeliveryStatus.QUEUED);
        });

        // Verify the domain event is marked as processed
        DomainEventDocument processed = domainEventRepository.findAll().get(0);
        assertThat(processed.isProcessed()).isTrue();
        assertThat(processed.getProcessedAt()).isNotNull();
    }

    @Test
    void eventStatusChangedEvent_createsNotificationDelivery() {
        DomainEventDocument event = new DomainEventDocument();
        event.setEventType("EventStatusChanged");
        event.setProcessed(false);
        event.setCreatedAt(Instant.now());
        event.setPayload(Map.of(
                "eventId", "outreach-event-002",
                "newStatus", "PUBLISHED",
                "eventName", "Community Cleanup Day",
                "notifyEmail", "admin@example.com",
                "notifyName", "Admin User"
        ));
        domainEventRepository.save(event);

        domainEventConsumer.pollDomainEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EmailDeliveryDocument> deliveries =
                    emailDeliveryRepository.findByEventId("outreach-event-002");
            assertThat(deliveries).hasSize(1);

            EmailDeliveryDocument delivery = deliveries.get(0);
            assertThat(delivery.getRecipientEmail()).isEqualTo("admin@example.com");
            assertThat(delivery.getSubject()).contains("Community Cleanup Day");
            assertThat(delivery.getSubject()).contains("PUBLISHED");
        });
    }

    @Test
    void volunteersImportedEvent_createsWelcomeEmails() {
        DomainEventDocument event = new DomainEventDocument();
        event.setEventType("VolunteersImported");
        event.setProcessed(false);
        event.setCreatedAt(Instant.now());
        event.setPayload(Map.of(
                "eventId", "outreach-event-003",
                "volunteers", List.of(
                        Map.of("email", "vol1@example.com", "name", "Volunteer One"),
                        Map.of("email", "vol2@example.com", "name", "Volunteer Two"),
                        Map.of("email", "vol3@example.com", "name", "Volunteer Three")
                )
        ));
        domainEventRepository.save(event);

        domainEventConsumer.pollDomainEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EmailDeliveryDocument> deliveries =
                    emailDeliveryRepository.findByEventId("outreach-event-003");
            assertThat(deliveries).hasSize(3);
            assertThat(deliveries)
                    .allMatch(d -> d.getSubject().contains("Welcome"));
        });
    }
}
