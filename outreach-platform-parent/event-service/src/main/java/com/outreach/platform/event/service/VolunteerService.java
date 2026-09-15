package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.EventEnrollmentEntity;
import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.mapper.VolunteerMapper;
import com.outreach.platform.event.model.dto.VolunteerDto;
import com.outreach.platform.event.model.dto.VolunteerHistoryDto;
import com.outreach.platform.event.model.dto.VolunteerProfileUpdateRequest;
import com.outreach.platform.event.repo.EventEnrollmentRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Business logic for the volunteer directory: listing/searching, profile detail,
 * profile/availability updates, and per-volunteer event participation history.
 */
@Service
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final EventEnrollmentRepository enrollmentRepository;
    private final VolunteerMapper volunteerMapper;

    @Inject
    public VolunteerService(VolunteerRepository volunteerRepository,
                             EventEnrollmentRepository enrollmentRepository,
                             VolunteerMapper volunteerMapper) {
        this.volunteerRepository = volunteerRepository;
        this.enrollmentRepository = enrollmentRepository;
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

    private VolunteerEntity findVolunteerOrThrow(String employeeId) {
        return volunteerRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Volunteer not found: " + employeeId));
    }
}
