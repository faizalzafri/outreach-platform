package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.DomainEventDocument;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.DomainEventRepository;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DomainEventConsumer scheduled poller.
 */
@ExtendWith(MockitoExtension.class)
class DomainEventConsumerTest {

    @Mock
    private DomainEventRepository domainEventRepository;

    @Mock
    private EmailDeliveryRepository emailDeliveryRepository;

    @Mock
    private EmailDispatchService emailDispatchService;

    private DomainEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DomainEventConsumer(domainEventRepository, emailDeliveryRepository, emailDispatchService);
    }

    @Test
    void pollDoesNothingWhenNoEventsFound() {
        when(domainEventRepository.findByEventTypeInAndProcessedFalse(anyList())).thenReturn(List.of());

        consumer.pollDomainEvents();

        verify(emailDeliveryRepository, never()).save(any());
        verify(emailDispatchService, never()).dispatchEmail(any());
    }

    @Test
    void handlesSendFeedbackEmailsEventByCreatingDeliveries() {
        DomainEventDocument event = new DomainEventDocument();
        event.setId("evt-1");
        event.setEventType("SendFeedbackEmails");
        event.setPayload(Map.of(
                "eventId", "event-123",
                "recipients", List.of(
                        Map.of("email", "alice@test.com", "name", "Alice"),
                        Map.of("email", "bob@test.com", "name", "Bob")
                )
        ));
        event.setProcessed(false);

        when(domainEventRepository.findByEventTypeInAndProcessedFalse(anyList())).thenReturn(List.of(event));
        when(emailDeliveryRepository.save(any(EmailDeliveryDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.pollDomainEvents();

        ArgumentCaptor<EmailDeliveryDocument> deliveryCaptor = ArgumentCaptor.forClass(EmailDeliveryDocument.class);
        verify(emailDeliveryRepository, times(2)).save(deliveryCaptor.capture());
        verify(emailDispatchService, times(2)).dispatchEmail(any());

        List<EmailDeliveryDocument> deliveries = deliveryCaptor.getAllValues();
        assertEquals("alice@test.com", deliveries.get(0).getRecipientEmail());
        assertEquals("bob@test.com", deliveries.get(1).getRecipientEmail());
        assertEquals(DeliveryStatus.PENDING, deliveries.get(0).getStatus());

        // Verify event marked as processed
        assertTrue(event.isProcessed());
        verify(domainEventRepository).save(event);
    }

    @Test
    void handlesVolunteersImportedEventByCreatingWelcomeEmails() {
        DomainEventDocument event = new DomainEventDocument();
        event.setId("evt-2");
        event.setEventType("VolunteersImported");
        event.setPayload(Map.of(
                "eventId", "event-456",
                "volunteers", List.of(
                        Map.of("email", "charlie@test.com", "name", "Charlie")
                )
        ));
        event.setProcessed(false);

        when(domainEventRepository.findByEventTypeInAndProcessedFalse(anyList())).thenReturn(List.of(event));
        when(emailDeliveryRepository.save(any(EmailDeliveryDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.pollDomainEvents();

        ArgumentCaptor<EmailDeliveryDocument> captor = ArgumentCaptor.forClass(EmailDeliveryDocument.class);
        verify(emailDeliveryRepository).save(captor.capture());
        assertEquals("charlie@test.com", captor.getValue().getRecipientEmail());
        assertTrue(captor.getValue().getSubject().contains("Welcome"));
        assertTrue(event.isProcessed());
    }

    @Test
    void handlesEventStatusChangedByCreatingLifecycleNotification() {
        DomainEventDocument event = new DomainEventDocument();
        event.setId("evt-3");
        event.setEventType("EventStatusChanged");
        event.setPayload(Map.of(
                "eventId", "event-789",
                "newStatus", "PUBLISHED",
                "eventName", "Tech Summit",
                "notifyEmail", "poc@test.com",
                "notifyName", "POC User"
        ));
        event.setProcessed(false);

        when(domainEventRepository.findByEventTypeInAndProcessedFalse(anyList())).thenReturn(List.of(event));
        when(emailDeliveryRepository.save(any(EmailDeliveryDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.pollDomainEvents();

        ArgumentCaptor<EmailDeliveryDocument> captor = ArgumentCaptor.forClass(EmailDeliveryDocument.class);
        verify(emailDeliveryRepository).save(captor.capture());
        assertEquals("poc@test.com", captor.getValue().getRecipientEmail());
        assertTrue(captor.getValue().getSubject().contains("PUBLISHED"));
        assertTrue(event.isProcessed());
    }

    @Test
    void continuesProcessingRemainingEventsWhenOneFailsAndDoesNotMarkFailedAsProcessed() {
        DomainEventDocument badEvent = new DomainEventDocument();
        badEvent.setId("evt-bad");
        badEvent.setEventType("SendFeedbackEmails");
        badEvent.setPayload(null); // Will cause NPE
        badEvent.setProcessed(false);

        DomainEventDocument goodEvent = new DomainEventDocument();
        goodEvent.setId("evt-good");
        goodEvent.setEventType("VolunteersImported");
        goodEvent.setPayload(Map.of(
                "eventId", "event-ok",
                "volunteers", List.of(Map.of("email", "x@test.com", "name", "X"))
        ));
        goodEvent.setProcessed(false);

        when(domainEventRepository.findByEventTypeInAndProcessedFalse(anyList()))
                .thenReturn(List.of(badEvent, goodEvent));
        when(emailDeliveryRepository.save(any(EmailDeliveryDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.pollDomainEvents();

        // Good event should still be processed
        assertTrue(goodEvent.isProcessed());
        // Bad event should not be marked as processed
        assertEquals(false, badEvent.isProcessed());
    }
}
