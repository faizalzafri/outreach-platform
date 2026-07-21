package com.outreach.platform.event.service;

import com.outreach.platform.event.config.EventServiceProperties;
import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.mapper.EventMapper;
import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.EventSearchCriteria;
import com.outreach.platform.event.model.dto.EventUpdateRequest;
import com.outreach.platform.event.model.dto.LifecycleStatsDto;
import com.outreach.platform.event.repo.EventRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business logic for event lifecycle management.
 */
@Service
public class EventService {

    private static final Map<EventStatus, Set<EventStatus>> VALID_TRANSITIONS = Map.of(
            EventStatus.DRAFT, Set.of(EventStatus.PUBLISHED, EventStatus.CANCELLED),
            EventStatus.PUBLISHED, Set.of(EventStatus.ACTIVE, EventStatus.CANCELLED),
            EventStatus.ACTIVE, Set.of(EventStatus.COMPLETED),
            EventStatus.COMPLETED, Set.of(EventStatus.ARCHIVED),
            EventStatus.ARCHIVED, Set.of(),
            EventStatus.CANCELLED, Set.of()
    );

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final EventServiceProperties properties;
    private final AtomicLong eventCodeSequence = new AtomicLong(System.currentTimeMillis() % 100000);

    @Inject
    public EventService(EventRepository eventRepository,
                        EventMapper eventMapper,
                        DomainEventPublisher domainEventPublisher,
                        EventServiceProperties properties) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.properties = properties;
    }

    /**
     * Creates a new event in DRAFT status with an auto-generated event code.
     */
    @Transactional
    public EventDto createEvent(EventCreateRequest request) {
        EventEntity entity = eventMapper.toEntity(request);
        entity.setStatus(EventStatus.DRAFT);
        entity.setEventCode(generateEventCode());
        entity.setRegisteredCount(0);
        entity.setAttendedCount(0);
        EventEntity saved = eventRepository.save(entity);
        return eventMapper.toDto(saved);
    }

    /**
     * Retrieves an event by its ID.
     *
     * @throws EventNotFoundException if event not found
     */
    @Transactional(readOnly = true)
    public EventDto getEvent(UUID eventId) {
        EventEntity entity = findEntityOrThrow(eventId);
        return eventMapper.toDto(entity);
    }

    /**
     * Updates event metadata. Only allowed for events not in terminal states.
     */
    @Transactional
    public EventDto updateEvent(UUID eventId, EventUpdateRequest request) {
        EventEntity entity = findEntityOrThrow(eventId);
        eventMapper.updateEntityFromRequest(request, entity);
        EventEntity saved = eventRepository.save(entity);
        return eventMapper.toDto(saved);
    }

    /**
     * Soft-deletes an event by transitioning it to ARCHIVED status.
     */
    @Transactional
    public void deleteEvent(UUID eventId) {
        EventEntity entity = findEntityOrThrow(eventId);
        entity.setStatus(EventStatus.ARCHIVED);
        entity.setArchivedAt(Instant.now());
        eventRepository.save(entity);
    }

    /**
     * Transitions an event's lifecycle status according to the state machine rules.
     *
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    @Transactional
    public EventDto transitionStatus(UUID eventId, EventStatus targetStatus) {
        EventEntity entity = findEntityOrThrow(eventId);
        EventStatus currentStatus = entity.getStatus();

        Set<EventStatus> allowedTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        entity.setStatus(targetStatus);
        applyStatusTimestamps(entity, targetStatus);

        EventEntity saved = eventRepository.save(entity);

        domainEventPublisher.publish("EventStatusChanged", Map.of(
                "eventId", eventId.toString(),
                "eventCode", entity.getEventCode(),
                "previousStatus", currentStatus.name(),
                "newStatus", targetStatus.name(),
                "transitionedAt", Instant.now().toString()
        ));

        return eventMapper.toDto(saved);
    }

    /**
     * Lists events with optional filtering and pagination.
     */
    @Transactional(readOnly = true)
    public Page<EventDto> listEvents(EventSearchCriteria criteria, Pageable pageable) {
        Page<EventEntity> page = eventRepository.findByFilters(
                criteria.status(),
                criteria.city(),
                criteria.category(),
                criteria.dateFrom(),
                criteria.dateTo(),
                pageable
        );
        return page.map(eventMapper::toDto);
    }

    /**
     * Full-text search across event name, city, and event code.
     */
    @Transactional(readOnly = true)
    public Page<EventDto> searchEvents(String query, Pageable pageable) {
        Page<EventEntity> page = eventRepository.search(query, pageable);
        return page.map(eventMapper::toDto);
    }

    /**
     * Calendar view: retrieves events within the specified date range.
     */
    @Transactional(readOnly = true)
    public Page<EventDto> getCalendarView(LocalDate from, LocalDate to, Pageable pageable) {
        Page<EventEntity> page = eventRepository.findByDateRange(from, to, pageable);
        return page.map(eventMapper::toDto);
    }

    /**
     * Returns aggregate counts of events grouped by lifecycle status.
     */
    @Transactional(readOnly = true)
    public LifecycleStatsDto getLifecycleStats() {
        return new LifecycleStatsDto(
                eventRepository.countByStatus(EventStatus.DRAFT),
                eventRepository.countByStatus(EventStatus.PUBLISHED),
                eventRepository.countByStatus(EventStatus.ACTIVE),
                eventRepository.countByStatus(EventStatus.COMPLETED),
                eventRepository.countByStatus(EventStatus.ARCHIVED),
                eventRepository.countByStatus(EventStatus.CANCELLED)
        );
    }

    private EventEntity findEntityOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private void applyStatusTimestamps(EventEntity entity, EventStatus targetStatus) {
        Instant now = Instant.now();
        switch (targetStatus) {
            case PUBLISHED -> entity.setPublishedAt(now);
            case COMPLETED -> entity.setCompletedAt(now);
            case ARCHIVED -> entity.setArchivedAt(now);
            default -> { }
        }
    }

    private String generateEventCode() {
        long seq = eventCodeSequence.incrementAndGet();
        return properties.eventCodePrefix() + "-" + String.format("%06d", seq % 1000000);
    }

    /**
     * Thrown when an event is not found by its ID.
     */
    public static class EventNotFoundException extends RuntimeException {
        public EventNotFoundException(UUID eventId) {
            super("Event not found: " + eventId);
        }
    }

    /**
     * Thrown when an invalid lifecycle status transition is attempted.
     */
    public static class InvalidStatusTransitionException extends RuntimeException {
        private final EventStatus from;
        private final EventStatus to;

        public InvalidStatusTransitionException(EventStatus from, EventStatus to) {
            super("Invalid status transition from " + from + " to " + to);
            this.from = from;
            this.to = to;
        }

        public EventStatus getFrom() {
            return from;
        }

        public EventStatus getTo() {
            return to;
        }
    }
}
