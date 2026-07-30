package com.outreach.platform.feedback.service;

import com.outreach.platform.feedback.entity.VolunteerFeedbackEntity;
import com.outreach.platform.feedback.mapper.FeedbackMapper;
import com.outreach.platform.feedback.model.FeedbackStatus;
import com.outreach.platform.feedback.model.dto.FeedbackDto;
import com.outreach.platform.feedback.model.dto.FeedbackSearchRequest;
import com.outreach.platform.feedback.model.dto.FeedbackStatusResponse;
import com.outreach.platform.feedback.model.dto.FeedbackSubmitRequest;
import com.outreach.platform.feedback.model.dto.FeedbackUpdateRequest;
import com.outreach.platform.feedback.repo.VolunteerFeedbackRepository;
import jakarta.inject.Inject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Core business logic for feedback CRUD, search, and status reporting. */
@Service
public class FeedbackService {

    private final VolunteerFeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    @Inject
    public FeedbackService(VolunteerFeedbackRepository feedbackRepository, FeedbackMapper feedbackMapper) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
    }

    @Transactional
    @CacheEvict(value = "feedbackByEvent", key = "#request.eventId()")
    public FeedbackDto submitFeedback(FeedbackSubmitRequest request) {
        Optional<VolunteerFeedbackEntity> existing =
                feedbackRepository.findByEventIdAndVolunteerId(request.eventId(), request.volunteerId());
        if (existing.isPresent()) {
            throw new FeedbackAlreadyExistsException(request.eventId(), request.volunteerId());
        }

        VolunteerFeedbackEntity entity = feedbackMapper.toEntity(request);
        VolunteerFeedbackEntity saved = feedbackRepository.save(entity);
        return feedbackMapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "feedbackByEvent", key = "#eventId")
    public FeedbackDto updateFeedback(UUID eventId, UUID employeeId, FeedbackUpdateRequest request) {
        VolunteerFeedbackEntity entity = feedbackRepository.findByEventIdAndVolunteerId(eventId, employeeId)
                .orElseThrow(() -> new FeedbackNotFoundException(eventId, employeeId));

        feedbackMapper.updateEntityFromRequest(request, entity);

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
    @Cacheable(value = "feedbackByEvent", key = "#eventId")
    public Page<FeedbackDto> listByEvent(UUID eventId, Pageable pageable) {
        return feedbackRepository.findByEventId(eventId, pageable)
                .map(feedbackMapper::toDto);
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
        return feedbackRepository.search(
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
        ).map(feedbackMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "feedbackByEvent", key = "#eventId")
    public void softDelete(UUID eventId, UUID employeeId) {
        VolunteerFeedbackEntity entity = feedbackRepository.findByEventIdAndVolunteerId(eventId, employeeId)
                .orElseThrow(() -> new FeedbackNotFoundException(eventId, employeeId));

        entity.setStatus(FeedbackStatus.ARCHIVED);
        feedbackRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return List.of(
                "Leadership", "Communication", "Teamwork",
                "Organization", "Impact", "Safety", "General"
        );
    }

    @Transactional(readOnly = true)
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
