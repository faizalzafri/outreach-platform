package com.outreach.platform.notification.controller;

import com.outreach.platform.notification.model.dto.ScheduleCreateRequest;
import com.outreach.platform.notification.model.dto.ScheduleDto;
import com.outreach.platform.notification.service.NotificationScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for notification scheduling — create, list, and cancel schedules.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/notifications/schedule")
@Tag(name = "Notification Scheduling", description = "Create, list, and cancel notification schedules")
public class ScheduleController {

    private final NotificationScheduleService scheduleService;

    @Inject
    public ScheduleController(NotificationScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * GET /notifications/schedule — list scheduled notifications (paginated).
     */
    @GetMapping
    public Page<ScheduleDto> listSchedules(Pageable pageable) {
        return scheduleService.listSchedules(pageable);
    }

    /**
     * POST /notifications/schedule — create a new scheduled notification.
     */
    @PostMapping
    public ResponseEntity<ScheduleDto> createSchedule(@Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleDto created = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * DELETE /notifications/schedule/{id} — cancel a scheduled notification.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelSchedule(@PathVariable UUID id) {
        scheduleService.cancelSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
