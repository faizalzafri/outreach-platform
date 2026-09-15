package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.EventEnrollmentEntity;
import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.mapper.VolunteerMapper;
import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.EmailStatus;
import com.outreach.platform.event.model.VolunteerAvailability;
import com.outreach.platform.event.model.dto.VolunteerDto;
import com.outreach.platform.event.model.dto.VolunteerHistoryDto;
import com.outreach.platform.event.model.dto.VolunteerImportRequest;
import com.outreach.platform.event.model.dto.VolunteerImportResponse;
import com.outreach.platform.event.model.dto.VolunteerProfileUpdateRequest;
import com.outreach.platform.event.repo.EventEnrollmentRepository;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

/**
 * Business logic for the volunteer directory: listing/searching, profile detail,
 * profile/availability updates, per-volunteer event participation history, and
 * bulk-import upsert+enrollment.
 */
@Service
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final EventEnrollmentRepository enrollmentRepository;
    private final EventRepository eventRepository;
    private final VolunteerMapper volunteerMapper;

    @Inject
    public VolunteerService(VolunteerRepository volunteerRepository,
                             EventEnrollmentRepository enrollmentRepository,
                             EventRepository eventRepository,
                             VolunteerMapper volunteerMapper) {
        this.volunteerRepository = volunteerRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.eventRepository = eventRepository;
        this.volunteerMapper = volunteerMapper;
    }

    @Transactional(readOnly = true)
    public Page<VolunteerDto> listVolunteers(String search, Pageable pageable) {
        Page<VolunteerEntity> page = (search == null || search.isBlank())
                ? volunteerRepository.findAll(pageable)
                : volunteerRepository.searchBySkillsOrLocation(search, pageable);
        return page.map(volunteerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public VolunteerDto getVolunteer(String employeeId) {
        return volunteerMapper.toDto(findVolunteerOrThrow(employeeId));
    }

    @Transactional
    public VolunteerDto updateProfile(String employeeId, VolunteerProfileUpdateRequest request) {
        VolunteerEntity entity = findVolunteerOrThrow(employeeId);
        volunteerMapper.updateEntityFromRequest(request, entity);
        VolunteerEntity saved = volunteerRepository.save(entity);
        return volunteerMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<VolunteerHistoryDto> getHistory(String employeeId, Pageable pageable) {
        VolunteerEntity volunteer = findVolunteerOrThrow(employeeId);
        Page<EventEnrollmentEntity> enrollments = enrollmentRepository.findByVolunteerId(volunteer.getId(), pageable);
        return enrollments.map(enrollment -> new VolunteerHistoryDto(
                enrollment.getEvent().getId(),
                enrollment.getEvent().getEventName(),
                enrollment.getEvent().getEventCode(),
                enrollment.getEvent().getEventDate(),
                enrollment.getEvent().getCity(),
                enrollment.getAttendanceStatus(),
                enrollment.getRegisteredAt()
        ));
    }

    /**
     * Upserts a volunteer profile by employeeId and enrolls it in the event identified by
     * eventCode, unless already enrolled (idempotent — re-importing the same file is a no-op
     * the second time, not an error).
     *
     * @throws NoSuchElementException if no event exists with the given eventCode
     */
    @Transactional
    public VolunteerImportResponse importVolunteer(VolunteerImportRequest request) {
        EventEntity event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new NoSuchElementException("Event not found for code: " + request.eventCode()));

        VolunteerEntity volunteer = volunteerRepository.findByEmployeeId(request.employeeId())
                .map(existing -> {
                    volunteerMapper.updateEntityFromImportRequest(request, existing);
                    return existing;
                })
                .orElseGet(() -> {
                    // Both columns have a DB-level DEFAULT, but Hibernate always includes every
                    // mapped column in the INSERT — an explicit null overrides the default rather
                    // than omitting the column, so these must be set here regardless.
                    VolunteerEntity created = volunteerMapper.toEntity(request);
                    created.setAvailability(VolunteerAvailability.AVAILABLE);
                    created.setTotalEventsParticipated(0);
                    return created;
                });
        volunteer = volunteerRepository.save(volunteer);

        boolean alreadyEnrolled = enrollmentRepository.existsByEventIdAndVolunteerId(event.getId(), volunteer.getId());
        if (!alreadyEnrolled) {
            EventEnrollmentEntity enrollment = new EventEnrollmentEntity();
            enrollment.setEvent(event);
            enrollment.setVolunteer(volunteer);
            enrollment.setAttendanceStatus(AttendanceStatus.REGISTERED);
            enrollment.setEmailStatus(EmailStatus.PENDING);
            enrollment.setRegisteredAt(Instant.now());
            enrollmentRepository.save(enrollment);
        }

        return new VolunteerImportResponse(volunteer.getId(), event.getId(), alreadyEnrolled);
    }

    private VolunteerEntity findVolunteerOrThrow(String employeeId) {
        return volunteerRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Volunteer not found: " + employeeId));
    }
}
