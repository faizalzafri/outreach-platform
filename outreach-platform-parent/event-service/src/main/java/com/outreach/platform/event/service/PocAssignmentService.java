package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.entity.PocAssignmentEntity;
import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.PocAssignRequest;
import com.outreach.platform.event.model.dto.PocAssignmentDto;
import com.outreach.platform.event.mapper.EventMapper;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.PocAssignmentRepository;
import com.outreach.platform.event.repo.UserRepository;
import jakarta.inject.Inject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Business logic for POC assignment management.
 */
@Service
public class PocAssignmentService {

    private final PocAssignmentRepository pocAssignmentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    @Inject
    public PocAssignmentService(PocAssignmentRepository pocAssignmentRepository,
                                EventRepository eventRepository,
                                UserRepository userRepository,
                                EventMapper eventMapper) {
        this.pocAssignmentRepository = pocAssignmentRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
    }

    /**
     * Assigns a POC user to an event.
     *
     * @throws NoSuchElementException if event or user not found
     * @throws DataIntegrityViolationException if POC is already assigned to this event
     */
    @Transactional
    public PocAssignmentDto assignPoc(UUID eventId, PocAssignRequest request) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + request.userId()));

        if (pocAssignmentRepository.existsByEventIdAndUserId(eventId, request.userId())) {
            throw new DataIntegrityViolationException(
                    "POC already assigned to event: userId=" + request.userId() + ", eventId=" + eventId);
        }

        PocAssignmentEntity entity = new PocAssignmentEntity();
        entity.setEvent(event);
        entity.setUser(user);
        entity.setAssignmentRole(request.role());
        entity.setAssignedAt(Instant.now());

        PocAssignmentEntity saved = pocAssignmentRepository.save(entity);
        return toDto(saved);
    }

    /**
     * Removes a POC assignment from an event.
     *
     * @throws NoSuchElementException if assignment not found
     */
    @Transactional
    public void removePoc(UUID eventId, UUID pocUserId) {
        PocAssignmentEntity entity = pocAssignmentRepository.findByEventIdAndUserId(eventId, pocUserId)
                .orElseThrow(() -> new NoSuchElementException(
                        "POC assignment not found: eventId=" + eventId + ", pocId=" + pocUserId));
        pocAssignmentRepository.delete(entity);
    }

    /**
     * Lists all events assigned to a specific POC user.
     */
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByPoc(UUID pocUserId) {
        List<PocAssignmentEntity> assignments = pocAssignmentRepository.findByUserId(pocUserId);
        return assignments.stream()
                .map(a -> eventMapper.toDto(a.getEvent()))
                .toList();
    }

    /**
     * Lists all POC assignments for a given event.
     */
    @Transactional(readOnly = true)
    public List<PocAssignmentDto> getAssignmentsForEvent(UUID eventId) {
        return pocAssignmentRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .toList();
    }

    private PocAssignmentDto toDto(PocAssignmentEntity entity) {
        return new PocAssignmentDto(
                entity.getId(),
                entity.getEvent().getId(),
                entity.getEvent().getEventName(),
                entity.getUser().getId(),
                entity.getUser().getUsername(),
                entity.getAssignmentRole(),
                entity.getAssignedAt(),
                entity.getAssignedBy()
        );
    }
}
