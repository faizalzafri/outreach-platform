package com.outreach.platform.event.service;

import com.outreach.platform.common.security.CurrentUser;
import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.EventEnrollmentEntity;
import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.EmailStatus;
import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.dto.AttendanceUpdateRequest;
import com.outreach.platform.event.model.dto.EnrollmentDto;
import com.outreach.platform.event.repo.EventEnrollmentRepository;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import jakarta.inject.Inject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business logic for volunteer enrollment in events.
 */
@Service
public class VolunteerEnrollmentService {

    /** Attendance is recorded once an event is under way. */
    private static final Set<EventStatus> ATTENDANCE_STATUSES = Set.of(EventStatus.ACTIVE, EventStatus.COMPLETED);

    private final EventEnrollmentRepository enrollmentRepository;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;
    private final EventVisibility visibility;

    @Inject
    public VolunteerEnrollmentService(EventEnrollmentRepository enrollmentRepository,
                                      EventRepository eventRepository,
                                      VolunteerRepository volunteerRepository,
                                      EventVisibility visibility) {
        this.enrollmentRepository = enrollmentRepository;
        this.eventRepository = eventRepository;
        this.volunteerRepository = volunteerRepository;
        this.visibility = visibility;
    }

    /**
     * Lists all enrolled volunteers for an event.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentDto> listEnrolledVolunteers(UUID eventId) {
        findEventOrThrow(eventId);
        visibility.requireVisible(eventId);
        return enrollmentRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Enrolls one or more volunteers in an event by employee IDs.
     *
     * @throws NoSuchElementException if event or any volunteer not found
     * @throws DataIntegrityViolationException if a volunteer is already enrolled
     */
    @CacheEvict(value = "eventCache", key = "#eventId")
    @Transactional
    public List<EnrollmentDto> enrollVolunteers(UUID eventId, List<String> employeeIds) {
        EventEntity event = findEventOrThrow(eventId);

        List<EnrollmentDto> results = new ArrayList<>();
        for (String employeeId : employeeIds) {
            VolunteerEntity volunteer = volunteerRepository.findByEmployeeId(employeeId)
                    .orElseThrow(() -> new NoSuchElementException("Volunteer not found: " + employeeId));

            if (enrollmentRepository.existsByEventIdAndVolunteerId(eventId, volunteer.getId())) {
                throw new DataIntegrityViolationException(
                        "Volunteer already enrolled: employeeId=" + employeeId + ", eventId=" + eventId);
            }

            EventEnrollmentEntity enrollment = new EventEnrollmentEntity();
            enrollment.setEvent(event);
            enrollment.setVolunteer(volunteer);
            enrollment.setAttendanceStatus(AttendanceStatus.REGISTERED);
            enrollment.setEmailStatus(EmailStatus.PENDING);
            enrollment.setRegisteredAt(Instant.now());

            EventEnrollmentEntity saved = enrollmentRepository.save(enrollment);
            results.add(toDto(saved));
        }

        enrollmentRepository.refreshCounts(event);
        return results;
    }

    /**
     * Removes a volunteer enrollment from an event.
     *
     * @throws NoSuchElementException if enrollment not found
     */
    @CacheEvict(value = "eventCache", key = "#eventId")
    @Transactional
    public void removeVolunteer(UUID eventId, String employeeId) {
        EventEntity event = findEventOrThrow(eventId);
        VolunteerEntity volunteer = volunteerRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Volunteer not found: " + employeeId));

        EventEnrollmentEntity enrollment = enrollmentRepository.findByEventIdAndVolunteerId(eventId, volunteer.getId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Enrollment not found: eventId=" + eventId + ", employeeId=" + employeeId));

        enrollmentRepository.delete(enrollment);
        enrollmentRepository.refreshCounts(event);
    }

    /**
     * Records attendance for enrolled volunteers. Managers may do it for any event, a POC only for
     * an event they are assigned to; either way only once the event is active or completed.
     */
    @CacheEvict(value = "eventCache", key = "#eventId")
    @Transactional
    public List<EnrollmentDto> recordAttendance(UUID eventId, List<AttendanceUpdateRequest.Entry> entries) {
        EventEntity event = findEventOrThrow(eventId);
        if (CurrentUser.isPocOnly() && !visibility.canEdit(eventId, CurrentUser.requireId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event's POCs, or teams with edit access, can record its attendance");
        }
        if (!ATTENDANCE_STATUSES.contains(event.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Attendance can be recorded once the event is active, not while it is " + event.getStatus());
        }

        Map<UUID, EventEnrollmentEntity> byVolunteer = enrollmentRepository.findByEventId(eventId).stream()
                .collect(Collectors.toMap(e -> e.getVolunteer().getId(), Function.identity()));
        String markedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        Instant now = Instant.now();
        for (AttendanceUpdateRequest.Entry entry : entries) {
            EventEnrollmentEntity enrollment = byVolunteer.get(entry.volunteerId());
            if (enrollment == null) {
                throw new NoSuchElementException("Volunteer " + entry.volunteerId() + " is not enrolled in this event");
            }
            enrollment.setAttendanceStatus(entry.status());
            enrollment.setAttendanceMarkedAt(now);
            enrollment.setMarkedBy(markedBy);
        }

        enrollmentRepository.refreshCounts(event);
        return byVolunteer.values().stream().map(this::toDto).toList();
    }

    private EventEntity findEventOrThrow(UUID eventId) {
        // findById() alone does not enforce tenant isolation on this codebase's Hibernate version —
        // see CLAUDE.md.
        Optional<EventEntity> event = TenantContext.isPresent()
                ? eventRepository.findByIdAndTenantId(eventId, TenantContext.getCurrentTenantId())
                : eventRepository.findById(eventId);
        return event.orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));
    }

    private EnrollmentDto toDto(EventEnrollmentEntity entity) {
        VolunteerEntity volunteer = entity.getVolunteer();
        return new EnrollmentDto(
                entity.getId(),
                entity.getEvent().getId(),
                volunteer.getId(),
                volunteer.getEmployeeId(),
                volunteer.getFullName(),
                entity.getAttendanceStatus(),
                entity.getEmailStatus(),
                entity.getRegisteredAt(),
                entity.getAttendanceMarkedAt()
        );
    }
}
