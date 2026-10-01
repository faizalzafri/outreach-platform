package com.outreach.platform.notification.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.notification.channel.ChannelDeliveryException;
import com.outreach.platform.notification.channel.MessageChannelSender;
import com.outreach.platform.notification.channel.MessageChannels;
import com.outreach.platform.notification.channel.OutboundMessage;
import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    /** Stands in for SMTP: records what would have been sent, or fails on demand. */
    private final RecordingChannel email = new RecordingChannel();

    private RabbitMqEventListener listener;

    @BeforeEach
    void setUp() {
        IdentityNotificationService identity = new IdentityNotificationService(
                new MessageChannels(List.of(email)), emailDeliveryRepository);
        listener = new RabbitMqEventListener(emailDeliveryRepository, emailDispatchService, identity);
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

    @Test
    void invitation_isSentWithTheLink_butTheStoredRecordHoldsNoSecret() {
        listener.onMessage(invitation("Meera <Iyer>"));

        assertThat(email.sent).singleElement().satisfies(sent -> {
            assertThat(sent.address()).isEqualTo("meera@example.com");
            assertThat(sent.subject()).isEqualTo("You're invited to Outreach Studio");
            assertThat(sent.htmlBody()).contains("https://auth.example/activate?token=SECRET")
                    // names from the event cannot inject markup
                    .contains("Meera &lt;Iyer&gt;").doesNotContain("<Iyer>");
            assertThat(sent.textBody()).contains("https://auth.example/activate?token=SECRET");
        });

        ArgumentCaptor<EmailDeliveryDocument> saved = ArgumentCaptor.forClass(EmailDeliveryDocument.class);
        verify(emailDeliveryRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(saved.getValue().isRedacted()).isTrue();
        assertThat(saved.getValue().getBody()).doesNotContain("SECRET");
        verify(emailDispatchService, never()).dispatchEmail(any(EmailDeliveryDocument.class));
    }

    @Test
    void oneTimeCode_subjectAndRecordNeverExposeTheCodeInStorage() {
        listener.onMessage(new DomainEventMessage("m-2", RabbitMqConstants.ROUTING_KEY_IDENTITY_OTP_ISSUED, Map.of(
                "email", "meera@example.com", "displayName", "Meera", "code", "482913",
                "purpose", "LOGIN", "channel", "EMAIL", "expiresAt", "2026-10-02T10:05:00Z")));

        assertThat(email.sent.getFirst().htmlBody()).contains("482913").contains("sign in");
        ArgumentCaptor<EmailDeliveryDocument> saved = ArgumentCaptor.forClass(EmailDeliveryDocument.class);
        verify(emailDeliveryRepository).save(saved.capture());
        assertThat(saved.getValue().getSubject()).isEqualTo("Verification code");
        assertThat(saved.getValue().getBody()).doesNotContain("482913");
    }

    @Test
    void failedSend_isRecordedAndRethrownSoRabbitRetries() {
        email.failNext = true;

        assertThatThrownBy(() -> listener.onMessage(invitation("Meera")))
                .isInstanceOf(ChannelDeliveryException.class);

        ArgumentCaptor<EmailDeliveryDocument> saved = ArgumentCaptor.forClass(EmailDeliveryDocument.class);
        verify(emailDeliveryRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DeliveryStatus.PERMANENTLY_FAILED);
        assertThat(saved.getValue().isRedacted()).isTrue();
    }

    private static DomainEventMessage invitation(String displayName) {
        return new DomainEventMessage("m-1", RabbitMqConstants.ROUTING_KEY_IDENTITY_USER_INVITED, Map.of(
                "userId", "u-1",
                "email", "meera@example.com",
                "displayName", displayName,
                "activationLink", "https://auth.example/activate?token=SECRET",
                "expiresAt", "2026-10-05T10:00:00Z",
                "invitedBy", "admin"));
    }

    private DomainEventMessage statusChanged(List<Map<String, String>> recipients) {
        return new DomainEventMessage("msg-1", "EventStatusChanged", Map.of(
                "eventId", "evt-1",
                "eventName", "Beach Cleanup",
                "newStatus", "ACTIVE",
                "recipients", recipients));
    }

    private static final class RecordingChannel implements MessageChannelSender {
        final List<OutboundMessage> sent = new ArrayList<>();
        boolean failNext;

        @Override
        public String channel() {
            return "EMAIL";
        }

        @Override
        public void send(OutboundMessage message) {
            if (failNext) {
                failNext = false;
                throw new ChannelDeliveryException("SMTP down");
            }
            sent.add(message);
        }
    }
}
