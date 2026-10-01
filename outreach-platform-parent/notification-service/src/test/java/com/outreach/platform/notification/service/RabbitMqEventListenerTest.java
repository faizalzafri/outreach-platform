package com.outreach.platform.notification.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMqEventListenerTest {

    @Mock
    private EmailDeliveryRepository emailDeliveryRepository;
    @Mock
    private EmailDispatchService emailDispatchService;

    private RabbitMqEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RabbitMqEventListener(emailDeliveryRepository, emailDispatchService);
    }

    @Test
    void eventStatusChanged_emailsEachAssignedPoc() {
        when(emailDeliveryRepository.save(any(EmailDeliveryDocument.class))).thenAnswer(i -> i.getArgument(0));

        listener.onMessage(statusChanged(List.of(
                Map.of("email", "poc1@example.com", "name", "poc_one"),
                Map.of("email", "poc2@example.com", "name", "poc_two"))));

        ArgumentCaptor<EmailDeliveryDocument> saved = ArgumentCaptor.forClass(EmailDeliveryDocument.class);
        verify(emailDeliveryRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(EmailDeliveryDocument::getRecipientEmail)
                .containsExactly("poc1@example.com", "poc2@example.com");
        assertThat(saved.getAllValues().get(0).getSubject()).isEqualTo("Event Update: Beach Cleanup — ACTIVE");
        verify(emailDispatchService, times(2)).dispatchEmail(any(EmailDeliveryDocument.class));
    }

    @Test
    void eventStatusChanged_withoutPocs_queuesNothing() {
        listener.onMessage(statusChanged(List.of()));

        verify(emailDeliveryRepository, never()).save(any(EmailDeliveryDocument.class));
        verify(emailDispatchService, never()).dispatchEmail(any(EmailDeliveryDocument.class));
    }

    private DomainEventMessage statusChanged(List<Map<String, String>> recipients) {
        return new DomainEventMessage("msg-1", "EventStatusChanged", Map.of(
                "eventId", "evt-1",
                "eventName", "Beach Cleanup",
                "newStatus", "ACTIVE",
                "recipients", recipients));
    }
}
