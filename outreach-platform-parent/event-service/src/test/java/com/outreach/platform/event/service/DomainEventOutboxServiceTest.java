package com.outreach.platform.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.platform.event.model.DomainEventDocument;
import com.outreach.platform.event.model.DomainEventStatus;
import com.outreach.platform.event.repo.DomainEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainEventOutboxService Unit Tests")
class DomainEventOutboxServiceTest {

    @Mock
    private DomainEventRepository domainEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DomainEventOutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new DomainEventOutboxService(domainEventRepository, rabbitTemplate, objectMapper);
    }

    @Test
    @DisplayName("save() persists event with PENDING status")
    void saveShouldPersistEventAsPending() {
        when(domainEventRepository.save(any(DomainEventDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DomainEventDocument result = outboxService.save("EventCreated", "{\"eventId\":\"123\"}");

        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isEqualTo("EventCreated");
        assertThat(result.getPayload()).isEqualTo("{\"eventId\":\"123\"}");
        assertThat(result.getStatus()).isEqualTo(DomainEventStatus.PENDING);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getRetryCount()).isZero();
        assertThat(result.getId()).isNotNull();

        verify(domainEventRepository).save(any(DomainEventDocument.class));
    }

    @Test
    @DisplayName("pollAndPublishPendingEvents() does nothing when no pending events exist")
    void pollShouldDoNothingWhenNoPendingEvents() {
        when(domainEventRepository.findByStatusOrderByCreatedAtAsc(DomainEventStatus.PENDING))
                .thenReturn(Collections.emptyList());

        outboxService.pollAndPublishPendingEvents();

        verify(domainEventRepository).findByStatusOrderByCreatedAtAsc(DomainEventStatus.PENDING);
        verify(domainEventRepository, never()).save(any(DomainEventDocument.class));
    }

    @Test
    @DisplayName("pollAndPublishPendingEvents() marks events as PUBLISHED")
    void pollShouldMarkEventsAsPublished() {
        DomainEventDocument pendingEvent = new DomainEventDocument("TestEvent", "{\"key\":\"value\"}");
        when(domainEventRepository.findByStatusOrderByCreatedAtAsc(DomainEventStatus.PENDING))
                .thenReturn(List.of(pendingEvent));
        when(domainEventRepository.save(any(DomainEventDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        outboxService.pollAndPublishPendingEvents();

        ArgumentCaptor<DomainEventDocument> captor = ArgumentCaptor.forClass(DomainEventDocument.class);
        verify(domainEventRepository).save(captor.capture());

        DomainEventDocument saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DomainEventStatus.PUBLISHED);
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("pollAndPublishPendingEvents() processes multiple pending events")
    void pollShouldProcessMultiplePendingEvents() {
        DomainEventDocument event1 = new DomainEventDocument("Event1", "{\"id\":1}");
        DomainEventDocument event2 = new DomainEventDocument("Event2", "{\"id\":2}");
        DomainEventDocument event3 = new DomainEventDocument("Event3", "{\"id\":3}");

        when(domainEventRepository.findByStatusOrderByCreatedAtAsc(DomainEventStatus.PENDING))
                .thenReturn(List.of(event1, event2, event3));
        when(domainEventRepository.save(any(DomainEventDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        outboxService.pollAndPublishPendingEvents();

        verify(domainEventRepository, times(3)).save(any(DomainEventDocument.class));
    }

    @Test
    @DisplayName("save() generates unique IDs for each event")
    void saveShouldGenerateUniqueIds() {
        when(domainEventRepository.save(any(DomainEventDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DomainEventDocument result1 = outboxService.save("Event1", "{}");
        DomainEventDocument result2 = outboxService.save("Event2", "{}");

        assertThat(result1.getId()).isNotEqualTo(result2.getId());
    }

    @Test
    @DisplayName("save() sets createdAt to current time")
    void saveShouldSetCreatedAtToCurrentTime() {
        when(domainEventRepository.save(any(DomainEventDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        DomainEventDocument result = outboxService.save("TestEvent", "{}");
        Instant after = Instant.now();

        assertThat(result.getCreatedAt()).isBetween(before, after);
    }
}
