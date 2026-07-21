package com.outreach.platform.notification.service;

import com.outreach.platform.notification.entity.NotificationScheduleEntity;
import com.outreach.platform.notification.model.ScheduleStatus;
import com.outreach.platform.notification.model.TriggerType;
import com.outreach.platform.notification.model.dto.ScheduleCreateRequest;
import com.outreach.platform.notification.model.dto.ScheduleDto;
import com.outreach.platform.notification.repo.NotificationScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationScheduleService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationScheduleServiceTest {

    @Mock
    private NotificationScheduleRepository scheduleRepository;

    private NotificationScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new NotificationScheduleService(scheduleRepository);
    }

    @Test
    void createScheduleSetsStatusToPendingAndPersists() {
        UUID templateId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        ScheduleCreateRequest request = new ScheduleCreateRequest(
                templateId, eventId, TriggerType.SCHEDULED, null, scheduledAt, null
        );

        NotificationScheduleEntity savedEntity = buildEntity(templateId, eventId, scheduledAt);
        when(scheduleRepository.save(any(NotificationScheduleEntity.class))).thenReturn(savedEntity);

        ScheduleDto result = scheduleService.createSchedule(request);

        assertNotNull(result);
        assertEquals(templateId, result.templateId());
        assertEquals(eventId, result.eventId());
        assertEquals(ScheduleStatus.PENDING, result.status());
        assertEquals(TriggerType.SCHEDULED, result.triggerType());
        verify(scheduleRepository).save(any(NotificationScheduleEntity.class));
    }

    @Test
    void listSchedulesReturnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        NotificationScheduleEntity entity = buildEntity(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        Page<NotificationScheduleEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(scheduleRepository.findAll(pageable)).thenReturn(page);

        Page<ScheduleDto> result = scheduleService.listSchedules(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(entity.getTemplateId(), result.getContent().get(0).templateId());
    }

    @Test
    void cancelScheduleSetsStatusToCancelled() {
        UUID scheduleId = UUID.randomUUID();
        NotificationScheduleEntity entity = buildEntity(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        entity.setId(scheduleId);
        entity.setStatus(ScheduleStatus.PENDING);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(entity));
        when(scheduleRepository.save(entity)).thenReturn(entity);

        scheduleService.cancelSchedule(scheduleId);

        assertEquals(ScheduleStatus.CANCELLED, entity.getStatus());
        verify(scheduleRepository).save(entity);
    }

    @Test
    void cancelScheduleThrowsWhenNotFound() {
        UUID scheduleId = UUID.randomUUID();
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> scheduleService.cancelSchedule(scheduleId));
    }

    @Test
    void cancelScheduleSkipsSaveWhenAlreadyCancelled() {
        UUID scheduleId = UUID.randomUUID();
        NotificationScheduleEntity entity = buildEntity(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        entity.setId(scheduleId);
        entity.setStatus(ScheduleStatus.CANCELLED);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(entity));

        scheduleService.cancelSchedule(scheduleId);

        verify(scheduleRepository, never()).save(any());
    }

    private NotificationScheduleEntity buildEntity(UUID templateId, UUID eventId, Instant scheduledAt) {
        NotificationScheduleEntity entity = new NotificationScheduleEntity();
        entity.setId(UUID.randomUUID());
        entity.setTemplateId(templateId);
        entity.setEventId(eventId);
        entity.setTriggerType(TriggerType.SCHEDULED);
        entity.setScheduledAt(scheduledAt);
        entity.setStatus(ScheduleStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        entity.setCreatedBy("admin");
        return entity;
    }
}
