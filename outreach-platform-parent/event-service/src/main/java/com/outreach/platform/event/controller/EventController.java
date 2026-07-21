package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.EventSearchCriteria;
import com.outreach.platform.event.model.dto.EventUpdateRequest;
import com.outreach.platform.event.model.dto.LifecycleStatsDto;
import com.outreach.platform.event.model.dto.StatusTransitionRequest;
import com.outreach.platform.event.service.EventService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for outreach event lifecycle management.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    @Inject
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Create a new event in DRAFT status.
     */
    @PostMapping
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventDto created = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * List events with optional filtering and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<EventDto>> listEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {

        EventSearchCriteria criteria = new EventSearchCriteria(status, city, category, dateFrom, dateTo, null);
        Page<EventDto> page = eventService.listEvents(criteria, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Get event details by ID.
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEvent(@PathVariable UUID eventId) {
        EventDto event = eventService.getEvent(eventId);
        return ResponseEntity.ok(event);
    }

    /**
     * Update event metadata.
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<EventDto> updateEvent(@PathVariable UUID eventId,
                                                @Valid @RequestBody EventUpdateRequest request) {
        EventDto updated = eventService.updateEvent(eventId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Transition event lifecycle status according to the state machine.
     */
    @PatchMapping("/{eventId}/status")
    public ResponseEntity<EventDto> transitionStatus(@PathVariable UUID eventId,
                                                     @Valid @RequestBody StatusTransitionRequest request) {
        EventDto updated = eventService.transitionStatus(eventId, request.targetStatus());
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft-delete an event (archives it).
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Full-text search across event name, city, and event code.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<EventDto>> searchEvents(
            @RequestParam String query,
            Pageable pageable) {
        Page<EventDto> page = eventService.searchEvents(query, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Calendar view: events within a date range.
     */
    @GetMapping("/calendar")
    public ResponseEntity<Page<EventDto>> calendarView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        Page<EventDto> page = eventService.getCalendarView(from, to, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Event lifecycle statistics: counts grouped by status.
     */
    @GetMapping("/lifecycle-stats")
    public ResponseEntity<LifecycleStatsDto> lifecycleStats() {
        LifecycleStatsDto stats = eventService.getLifecycleStats();
        return ResponseEntity.ok(stats);
    }
}
