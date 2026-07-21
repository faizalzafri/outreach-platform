package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.EventSearchCriteria;
import com.outreach.platform.event.model.dto.EventUpdateRequest;
import com.outreach.platform.event.model.dto.LifecycleStatsDto;
import com.outreach.platform.event.model.dto.StatusTransitionRequest;
import com.outreach.platform.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Event Management", description = "Outreach event lifecycle management, search, and calendar operations")
public class EventController {

    private final EventService eventService;

    @Inject
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Create a new event", description = "Creates a new outreach event in DRAFT status")
    @PostMapping
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventDto created = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List events", description = "Lists events with optional status, city, category, and date range filters")
    @GetMapping
    public ResponseEntity<Page<EventDto>> listEvents(
            @Parameter(description = "Filter by lifecycle status") @RequestParam(required = false) EventStatus status,
            @Parameter(description = "Filter by city") @RequestParam(required = false) String city,
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Start of date range") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End of date range") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {

        EventSearchCriteria criteria = new EventSearchCriteria(status, city, category, dateFrom, dateTo, null);
        Page<EventDto> page = eventService.listEvents(criteria, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Get event details", description = "Retrieves full details of an event by its ID")
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEvent(@Parameter(description = "Event UUID") @PathVariable UUID eventId) {
        EventDto event = eventService.getEvent(eventId);
        return ResponseEntity.ok(event);
    }

    @Operation(summary = "Update event", description = "Updates event metadata")
    @PutMapping("/{eventId}")
    public ResponseEntity<EventDto> updateEvent(@Parameter(description = "Event UUID") @PathVariable UUID eventId,
                                                @Valid @RequestBody EventUpdateRequest request) {
        EventDto updated = eventService.updateEvent(eventId, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Transition event status", description = "Transitions event lifecycle status according to the state machine")
    @PatchMapping("/{eventId}/status")
    public ResponseEntity<EventDto> transitionStatus(@Parameter(description = "Event UUID") @PathVariable UUID eventId,
                                                     @Valid @RequestBody StatusTransitionRequest request) {
        EventDto updated = eventService.transitionStatus(eventId, request.targetStatus());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete event", description = "Soft-deletes an event by archiving it")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@Parameter(description = "Event UUID") @PathVariable UUID eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search events", description = "Full-text search across event name, city, and event code")
    @GetMapping("/search")
    public ResponseEntity<Page<EventDto>> searchEvents(
            @Parameter(description = "Search query") @RequestParam String query,
            Pageable pageable) {
        Page<EventDto> page = eventService.searchEvents(query, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Calendar view", description = "Events within a date range for calendar display")
    @GetMapping("/calendar")
    public ResponseEntity<Page<EventDto>> calendarView(
            @Parameter(description = "Range start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Range end date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        Page<EventDto> page = eventService.getCalendarView(from, to, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Lifecycle statistics", description = "Event counts grouped by lifecycle status")
    @GetMapping("/lifecycle-stats")
    public ResponseEntity<LifecycleStatsDto> lifecycleStats() {
        LifecycleStatsDto stats = eventService.getLifecycleStats();
        return ResponseEntity.ok(stats);
    }
}
