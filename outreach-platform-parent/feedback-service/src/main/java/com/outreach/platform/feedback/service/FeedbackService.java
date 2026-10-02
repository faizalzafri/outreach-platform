package com.outreach.platform.feedback.service;

import com.outreach.platform.common.security.CurrentUser;
import com.outreach.platform.feedback.client.EventServiceClient;
import com.outreach.platform.feedback.entity.VolunteerFeedbackEntity;
import com.outreach.platform.feedback.mapper.FeedbackMapper;
import com.outreach.platform.feedback.model.FeedbackCategory;
import com.outreach.platform.feedback.model.FeedbackStatus;
import com.outreach.platform.feedback.model.dto.FeedbackDto;
import com.outreach.platform.feedback.model.dto.FeedbackSearchRequest;
import com.outreach.platform.feedback.model.dto.FeedbackStatusResponse;
import com.outreach.platform.feedback.model.dto.FeedbackSubmitRequest;
import com.outreach.platform.feedback.model.dto.FeedbackUpdateRequest;
import com.outreach.platform.feedback.repo.VolunteerFeedbackRepository;
import feign.FeignException;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Core business logic for feedback CRUD, search, and status reporting. */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    /** Event statuses in which feedback may be given. */
    private static final Set<String> OPEN_FOR_FEEDBACK = Set.of("ACTIVE", "COMPLETED");

    private final VolunteerFeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;
    private final EventServiceClient eventServiceClient;

    @Inject
    public FeedbackService(VolunteerFeedbackRepository feedbackRepository, FeedbackMapper feedbackMapper,
                           EventServiceClient eventServiceClient) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
        this.eventServiceClient = eventServiceClient;
    }

    @Transactional
    public FeedbackDto submitFeedback(FeedbackSubmitRequest request) {
        requireOpenForFeedback(request.eventId());
        Optional<VolunteerFeedbackEntity> existing =
                feedbackRepository.findByEventIdAndVolunteerId(request.eventId(), request.volunteerId());
        if (existing.isPresent()) {
            throw new FeedbackAlreadyExistsException();
        }

        VolunteerFeedbackEntity entity = feedbackMapper.toEntity(request);
        entity.setCategory(FeedbackCategory.normalize(request.category()));
        VolunteerFeedbackEntity saved = feedbackRepository.save(entity);
        return feedbackMapper.toDto(saved);
    }

    @Transactional
    public FeedbackDto updateFeedback(UUID eventId, UUID employeeId, FeedbackUpdateRequest request) {
        VolunteerFeedbackEntity entity = feedbackRepository.findByEventIdAndVolunteerId(eventId, employeeId)
                .orElseThrow(() -> new FeedbackNotFoundException(eventId, employeeId));

        feedbackMapper.updateEntityFromRequest(request, entity);
        if (request.category() != null) {
            entity.setCategory(FeedbackCategory.normalize(request.category()));
        }

        try {
            VolunteerFeedbackEntity saved = feedbackRepository.save(entity);
            return feedbackMapper.toDto(saved);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new FeedbackConflictException(eventId, employeeId);
        }
    }

    @Transactional(readOnly = true)
    public FeedbackDto getFeedback(UUID eventId, UUID employeeId) {
        VolunteerFeedbackEntity entity = feedbackRepository.findByEventIdAndVolunteerId(eventId, employeeId)
                .orElseThrow(() -> new FeedbackNotFoundException(eventId, employeeId));
        return feedbackMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDto> listByEvent(UUID eventId, Pageable pageable) {
        return withNames(feedbackRepository.findByEventId(eventId, pageable));
    }

    @Transactional(readOnly = true)
    public FeedbackStatusResponse getEventStatus(UUID eventId) {
        long total = feedbackRepository.countByEventId(eventId);
        long submitted = feedbackRepository.countByEventIdAndStatus(eventId, FeedbackStatus.SUBMITTED);
        long reviewed = feedbackRepository.countByEventIdAndStatus(eventId, FeedbackStatus.REVIEWED);
        long flagged = feedbackRepository.countByEventIdAndStatus(eventId, FeedbackStatus.FLAGGED);
        long archived = feedbackRepository.countByEventIdAndStatus(eventId, FeedbackStatus.ARCHIVED);
        double averageScore = feedbackRepository.averageScoreByEventId(eventId);
        double completionRate = total > 0 ? (double) (reviewed + submitted) / total * 100.0 : 0.0;

        return new FeedbackStatusResponse(eventId, total, submitted, reviewed, flagged, archived, averageScore, completionRate);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDto> search(FeedbackSearchRequest request, Pageable pageable) {
        return withNames(feedbackRepository.search(
                request.eventId(),
                request.employeeId(),
                request.category(),
                request.sentiment(),
                request.status(),
                request.minScore(),
                request.maxScore(),
                request.dateFrom(),
                request.dateTo(),
                pageable
        ));
    }

    /**
     * Feedback is given for events under way or finished, and by a POC only for their own events.
     * event-service is asked; if it cannot answer, nothing is accepted.
     */
    private void requireOpenForFeedback(UUID eventId) {
        boolean pocOnly = CurrentUser.isPocOnly();
        EventServiceClient.FeedbackEligibility eligibility;
        try {
            eligibility = eventServiceClient.feedbackEligibility(eventId, pocOnly ? CurrentUser.requireId() : null);
        } catch (FeignException.NotFound ex) {
            throw new NoSuchElementException("Event not found: " + eventId);
        } catch (FeignException ex) {
            log.warn("Could not check event {} with event-service: {}", eventId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Feedback cannot be accepted right now; please try again shortly");
        }
        if (!OPEN_FOR_FEEDBACK.contains(eligibility.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Feedback opens once the event is active; this event is " + eligibility.status());
        }
        if (pocOnly && !eligibility.assigned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the event's POCs can give feedback for it");
        }
    }

    /**
     * Fills in event and volunteer names with one batched call each. Names are for display only,
     * so if event-service cannot answer the page is still returned, without them.
     */
    private Page<FeedbackDto> withNames(Page<VolunteerFeedbackEntity> page) {
        Set<UUID> eventIds = page.stream().map(VolunteerFeedbackEntity::getEventId).collect(Collectors.toSet());
        Set<UUID> volunteerIds = page.stream().filter(f -> !f.isAnonymous())
                .map(VolunteerFeedbackEntity::getVolunteerId).collect(Collectors.toSet());
        Map<UUID, String> eventNames = Map.of();
        Map<UUID, String> volunteerNames = Map.of();
        if (!page.isEmpty()) {
            try {
                eventNames = eventServiceClient.eventNames(eventIds);
                volunteerNames = volunteerIds.isEmpty() ? Map.of() : eventServiceClient.volunteerNames(volunteerIds);
            } catch (FeignException ex) {
                log.warn("Feedback shown without names; event-service did not answer: {}", ex.getMessage());
            }
        }
        Map<UUID, String> events = eventNames;
        Map<UUID, String> volunteers = volunteerNames;
        return page.map(f -> feedbackMapper.toDto(f, events.get(f.getEventId()),
                f.isAnonymous() ? null : volunteers.get(f.getVolunteerId())));
    }

    @Transactional
    public void softDelete(UUID eventId, UUID employeeId) {
        VolunteerFeedbackEntity entity = feedbackRepository.findByEventIdAndVolunteerId(eventId, employeeId)
                .orElseThrow(() -> new FeedbackNotFoundException(eventId, employeeId));

        entity.setStatus(FeedbackStatus.ARCHIVED);
        feedbackRepository.save(entity);
    }

    public List<String> getCategories() {
        return FeedbackCategory.labels();
    }

    public List<String> getTags() {
        return List.of(
                "excellent", "needs-improvement", "first-time",
                "recurring", "high-impact", "community", "education",
                "environment", "health", "technology"
        );
    }

    @Transactional(readOnly = true)
    public List<VolunteerFeedbackEntity> listAllByEvent(UUID eventId) {
        return feedbackRepository.findByEventId(eventId, Pageable.unpaged()).getContent();
    }
}
