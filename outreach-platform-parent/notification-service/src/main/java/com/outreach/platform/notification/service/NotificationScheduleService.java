package com.outreach.platform.notification.service;

import com.outreach.platform.notification.entity.NotificationScheduleEntity;
import com.outreach.platform.notification.model.ScheduleStatus;
import com.outreach.platform.notification.model.dto.ScheduleCreateRequest;
import com.outreach.platform.notification.model.dto.ScheduleDto;
import com.outreach.platform.notification.repo.NotificationScheduleRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service managing notification schedule lifecycle — creation, listing, and cancellation.
 */
@Service
public class NotificationScheduleService {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduleService.class);

    private final NotificationScheduleRepository scheduleRepository;

    @Inject
    public NotificationScheduleService(NotificationScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Creates a new notification schedule from the given request.
     */
    @Transactional
    public ScheduleDto createSchedule(ScheduleCreateRequest request) {
        NotificationScheduleEntity entity = new NotificationScheduleEntity();
        entity.setTemplateId(request.templateId());
        entity.setEventId(request.eventId());
        entity.setTriggerType(request.triggerType());
        entity.setCronExpression(request.cronExpression());
        entity.setScheduledAt(request.scheduledAt());
        entity.setStatus(ScheduleStatus.PENDING);
        entity.setRecipientFilter(request.recipientFilter());

        NotificationScheduleEntity saved = scheduleRepository.save(entity);
        log.info("Created notification schedule: id={}, triggerType={}", saved.getId(), saved.getTriggerType());
        return toDto(saved);
    }

    /**
     * Lists all schedules with pagination support.
     */
    public Page<ScheduleDto> listSchedules(Pageable pageable) {
        return scheduleRepository.findAll(pageable).map(this::toDto);
    }

    /**
     * Cancels a schedule by setting its status to CANCELLED.
     */
    @Transactional
    public void cancelSchedule(UUID id) {
        NotificationScheduleEntity entity = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));

        if (entity.getStatus() == ScheduleStatus.CANCELLED) {
            log.warn("Schedule {} is already cancelled", id);
            return;
        }

        entity.setStatus(ScheduleStatus.CANCELLED);
        scheduleRepository.save(entity);
        log.info("Cancelled notification schedule: id={}", id);
    }

    private ScheduleDto toDto(NotificationScheduleEntity entity) {
        return new ScheduleDto(
                entity.getId(),
                entity.getTemplateId(),
                entity.getEventId(),
                entity.getTriggerType(),
                entity.getCronExpression(),
                entity.getScheduledAt(),
                entity.getStatus(),
                entity.getRecipientFilter(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }
}
