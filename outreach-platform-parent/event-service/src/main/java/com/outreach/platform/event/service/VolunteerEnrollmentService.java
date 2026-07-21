package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.EventEnrollmentEntity;
import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.EmailStatus;
import com.outreach.platform.event.model.dto.AttendanceBreakdownDto;
import com.outreach.platform.event.model.dto.EnrollmentDto;
import com.outreach.platform.event.repo.EventEnrollmentRepository;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import jakarta.inject.Inject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Business logic for volunteer enrollment in events.
 */
@Service
public class VolunteerEnrollmentService {

    private final EventEnrollmentRepository enrollmentRepository;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;

    @Inject
    public VolunteerEnrollmentService(EventEnrollmentRepository enrollmentRepository,
                                      EventRepository eventRepository,
                                      VolunteerRepository volunteerRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.eventRepository = eventRepository;
        this.volunteerRepository = volunteerRepository;
    }

    /**
     * Lists all enrolled volunteers for an event.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentDto> listEnrolledVolunteers(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NoSuchElementException("Event not found: " + eventId);
        }
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
    @Transactional
    public List<EnrollmentDto> enrollVolunteers(UUID eventId, List<String> employeeIds) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));

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

        // Update event registered count
        event.setRegisteredCount(
                (event.getRegisteredCount() != null ? event.getRegisteredCount() : 0) + employeeIds.size());
        eventRepository.save(event);

        return results;
    }

    /**
     * Removes a volunteer enrollment from an event.
     *
     * @throws NoSuchElementException if enrollment not found
     */
    @Transactional
    public void removeVolunteer(UUID eventId, String employeeId) {
        VolunteerEntity volunteer = volunteerRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Volunteer not found: " + employeeId));

        EventEnrollmentEntity enrollment = enrollmentRepository.findByEventIdAndVolunteerId(eventId, volunteer.getId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Enrollment not found: eventId=" + eventId + ", employeeId=" + employeeId));

        enrollmentRepository.delete(enrollment);

        // Decrement registered count
        eventRepository.findById(eventId).ifPresent(event -> {
            int current = event.getRegisteredCount() != null ? event.getRegisteredCount() : 0;
            event.setRegisteredCount(Math.max(0, current - 1));
            eventRepository.save(event);
        });
    }

    /**
     * Returns attendance breakdown for an event (count by status).
     */
    @Transactional(readOnly = true)
    public AttendanceBreakdownDto getAttendanceBreakdown(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NoSuchElementException("Event not found: " + eventId);
        }
        long registered = enrollmentRepository.countByEventIdAndAttendanceStatus(eventId, AttendanceStatus.REGISTERED);
        long attended = enrollmentRepository.countByEventIdAndAttendanceStatus(eventId, AttendanceStatus.ATTENDED);
        long notAttended = enrollmentRepository.countByEventIdAndAttendanceStatus(eventId, AttendanceStatus.NOT_ATTENDED);
        long unregistered = enrollmentRepository.countByEventIdAndAttendanceStatus(eventId, AttendanceStatus.UNREGISTERED);
        return new AttendanceBreakdownDto(registered + attended + notAttended, attended, notAttended, unregistered);
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
