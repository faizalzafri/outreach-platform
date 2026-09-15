package com.outreach.platform.event.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.EventEnrollmentEntity;
import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.dto.EnrollmentDto;
import com.outreach.platform.event.repo.EventEnrollmentRepository;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VolunteerEnrollmentService Unit Tests")
class VolunteerEnrollmentServiceTest {

    @Mock
    private EventEnrollmentRepository enrollmentRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private VolunteerRepository volunteerRepository;

    private VolunteerEnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new VolunteerEnrollmentService(enrollmentRepository, eventRepository, volunteerRepository);
        // Production requests always have TenantContext populated by TenantContextFilter — set it
        // here too so these tests exercise the tenant-scoped findByIdAndTenantId path. See
        // docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        TenantContext.setCurrentTenantId(UUID.randomUUID());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("listEnrolledVolunteers should map enrollments to DTOs")
    void listEnrolledVolunteersShouldMapToDtos() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(enrollmentRepository.findByEventId(eventId)).thenReturn(List.of(buildEnrollment(eventId)));

        List<EnrollmentDto> result = enrollmentService.listEnrolledVolunteers(eventId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).employeeId()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("listEnrolledVolunteers should throw when event does not exist")
    void listEnrolledVolunteersShouldThrowWhenEventMissing() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(false);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> enrollmentService.listEnrolledVolunteers(eventId));
    }

    @Test
    @DisplayName("enrollVolunteers should save an enrollment and bump registeredCount")
    void enrollVolunteersShouldSaveAndBumpCount() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setId(eventId);
        event.setRegisteredCount(2);
        VolunteerEntity volunteer = buildVolunteer("EMP001");

        when(eventRepository.findByIdAndTenantId(eq(eventId), any())).thenReturn(Optional.of(event));
        when(volunteerRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(volunteer));
        when(enrollmentRepository.existsByEventIdAndVolunteerId(eventId, volunteer.getId())).thenReturn(false);
        when(enrollmentRepository.save(any(EventEnrollmentEntity.class))).thenAnswer(inv -> {
            EventEnrollmentEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        List<EnrollmentDto> result = enrollmentService.enrollVolunteers(eventId, List.of("EMP001"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).employeeId()).isEqualTo("EMP001");
        assertThat(event.getRegisteredCount()).isEqualTo(3);
        verify(enrollmentRepository).save(any(EventEnrollmentEntity.class));
    }

    @Test
    @DisplayName("enrollVolunteers should throw when volunteer does not exist")
    void enrollVolunteersShouldThrowWhenVolunteerMissing() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setId(eventId);

        when(eventRepository.findByIdAndTenantId(eq(eventId), any())).thenReturn(Optional.of(event));
        when(volunteerRepository.findByEmployeeId("NOPE")).thenReturn(Optional.empty());

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> enrollmentService.enrollVolunteers(eventId, List.of("NOPE")));
    }

    @Test
    @DisplayName("enrollVolunteers should throw on duplicate enrollment")
    void enrollVolunteersShouldThrowOnDuplicate() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setId(eventId);
        VolunteerEntity volunteer = buildVolunteer("EMP001");

        when(eventRepository.findByIdAndTenantId(eq(eventId), any())).thenReturn(Optional.of(event));
        when(volunteerRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(volunteer));
        when(enrollmentRepository.existsByEventIdAndVolunteerId(eventId, volunteer.getId())).thenReturn(true);

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> enrollmentService.enrollVolunteers(eventId, List.of("EMP001")));
    }

    @Test
    @DisplayName("removeVolunteer should delete the enrollment and decrement registeredCount")
    void removeVolunteerShouldDeleteAndDecrementCount() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setId(eventId);
        event.setRegisteredCount(3);
        VolunteerEntity volunteer = buildVolunteer("EMP001");
        EventEnrollmentEntity enrollment = buildEnrollment(eventId, volunteer);

        when(volunteerRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(volunteer));
        when(enrollmentRepository.findByEventIdAndVolunteerId(eventId, volunteer.getId()))
                .thenReturn(Optional.of(enrollment));
        when(eventRepository.findByIdAndTenantId(eq(eventId), any())).thenReturn(Optional.of(event));

        enrollmentService.removeVolunteer(eventId, "EMP001");

        verify(enrollmentRepository).delete(enrollment);
        assertThat(event.getRegisteredCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("removeVolunteer should throw when the enrollment does not exist")
    void removeVolunteerShouldThrowWhenEnrollmentMissing() {
        UUID eventId = UUID.randomUUID();
        VolunteerEntity volunteer = buildVolunteer("EMP001");

        when(volunteerRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(volunteer));
        when(enrollmentRepository.findByEventIdAndVolunteerId(eventId, volunteer.getId()))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> enrollmentService.removeVolunteer(eventId, "EMP001"));
    }

    private VolunteerEntity buildVolunteer(String employeeId) {
        VolunteerEntity volunteer = new VolunteerEntity();
        volunteer.setId(UUID.randomUUID());
        volunteer.setEmployeeId(employeeId);
        volunteer.setFullName("Test Volunteer");
        return volunteer;
    }

    private EventEnrollmentEntity buildEnrollment(UUID eventId) {
        return buildEnrollment(eventId, buildVolunteer("EMP001"));
    }

    private EventEnrollmentEntity buildEnrollment(UUID eventId, VolunteerEntity volunteer) {
        EventEntity event = new EventEntity();
        event.setId(eventId);

        EventEnrollmentEntity enrollment = new EventEnrollmentEntity();
        enrollment.setId(UUID.randomUUID());
        enrollment.setEvent(event);
        enrollment.setVolunteer(volunteer);
        enrollment.setAttendanceStatus(AttendanceStatus.REGISTERED);
        return enrollment;
    }
}
