package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.EventEnrollmentEntity;
import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.mapper.VolunteerMapper;
import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.VolunteerAvailability;
import com.outreach.platform.event.model.dto.VolunteerDto;
import com.outreach.platform.event.model.dto.VolunteerHistoryDto;
import com.outreach.platform.event.model.dto.VolunteerImportRequest;
import com.outreach.platform.event.model.dto.VolunteerImportResponse;
import com.outreach.platform.event.model.dto.VolunteerProfileUpdateRequest;
import com.outreach.platform.event.repo.EventEnrollmentRepository;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VolunteerService Unit Tests")
class VolunteerServiceTest {

    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private EventEnrollmentRepository enrollmentRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private VolunteerMapper volunteerMapper;

    private VolunteerService volunteerService;

    @BeforeEach
    void setUp() {
        volunteerService = new VolunteerService(volunteerRepository, enrollmentRepository, eventRepository, volunteerMapper);
    }

    @Test
    @DisplayName("listVolunteers without search delegates to findAll")
    void listVolunteersWithoutSearchUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        VolunteerEntity volunteer = buildVolunteer("EMP001");
        VolunteerDto dto = buildDto("EMP001");
        when(volunteerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(volunteer)));
        when(volunteerMapper.toDto(volunteer)).thenReturn(dto);

        Page<VolunteerDto> result = volunteerService.listVolunteers(null, pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    @DisplayName("listVolunteers with a blank search still uses findAll, not search")
    void listVolunteersWithBlankSearchUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(volunteerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        volunteerService.listVolunteers("   ", pageable);

        verify(volunteerRepository).findAll(pageable);
        verifyNoMoreInteractions(volunteerRepository);
    }

    @Test
    @DisplayName("listVolunteers with a search term delegates to searchBySkillsOrLocation")
    void listVolunteersWithSearchDelegatesToSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        VolunteerEntity volunteer = buildVolunteer("EMP002");
        VolunteerDto dto = buildDto("EMP002");
        when(volunteerRepository.searchBySkillsOrLocation(eq("mumbai"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(volunteer)));
        when(volunteerMapper.toDto(volunteer)).thenReturn(dto);

        Page<VolunteerDto> result = volunteerService.listVolunteers("mumbai", pageable);

        assertThat(result.getContent()).containsExactly(dto);
        verify(volunteerRepository, org.mockito.Mockito.never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getVolunteer throws when employee ID is unknown")
    void getVolunteerThrowsWhenMissing() {
        when(volunteerRepository.findByEmployeeId("NOPE")).thenReturn(Optional.empty());

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> volunteerService.getVolunteer("NOPE"));
    }

    @Test
    @DisplayName("updateProfile maps the request onto the existing entity and saves it")
    void updateProfileMapsAndSaves() {
        VolunteerEntity entity = buildVolunteer("EMP003");
        VolunteerDto dto = buildDto("EMP003");
        when(volunteerRepository.findByEmployeeId("EMP003")).thenReturn(Optional.of(entity));
        when(volunteerRepository.save(entity)).thenReturn(entity);
        when(volunteerMapper.toDto(entity)).thenReturn(dto);

        VolunteerProfileUpdateRequest request = new VolunteerProfileUpdateRequest(
                null, null, null, null, null, null, null, VolunteerAvailability.ON_LEAVE);

        VolunteerDto result = volunteerService.updateProfile("EMP003", request);

        verify(volunteerMapper).updateEntityFromRequest(request, entity);
        verify(volunteerRepository).save(entity);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("getHistory maps enrollments through their event into VolunteerHistoryDto")
    void getHistoryMapsEnrollmentsToHistoryDtos() {
        VolunteerEntity volunteer = buildVolunteer("EMP004");
        volunteer.setId(UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 5);

        EventEntity event = new EventEntity();
        event.setId(UUID.randomUUID());
        event.setEventName("Beach Cleanup");
        event.setEventCode("EVT-001");
        event.setEventDate(LocalDate.of(2024, 4, 15));
        event.setCity("Chennai");

        EventEnrollmentEntity enrollment = new EventEnrollmentEntity();
        enrollment.setEvent(event);
        enrollment.setAttendanceStatus(AttendanceStatus.ATTENDED);

        when(volunteerRepository.findByEmployeeId("EMP004")).thenReturn(Optional.of(volunteer));
        when(enrollmentRepository.findByVolunteerId(volunteer.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(enrollment)));

        Page<VolunteerHistoryDto> result = volunteerService.getHistory("EMP004", pageable);

        assertThat(result.getContent()).hasSize(1);
        VolunteerHistoryDto historyDto = result.getContent().get(0);
        assertThat(historyDto.eventName()).isEqualTo("Beach Cleanup");
        assertThat(historyDto.eventCode()).isEqualTo("EVT-001");
        assertThat(historyDto.city()).isEqualTo("Chennai");
        assertThat(historyDto.attendanceStatus()).isEqualTo(AttendanceStatus.ATTENDED);
    }

    @Test
    @DisplayName("importVolunteer creates a new volunteer and enrolls it when neither exists")
    void importVolunteerCreatesAndEnrolls() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setId(eventId);
        event.setEventCode("EVT-001");

        VolunteerEntity newVolunteer = buildVolunteer("EMP020");
        UUID volunteerId = UUID.randomUUID();

        VolunteerImportRequest request = new VolunteerImportRequest(
                "EMP020", "New Volunteer", "new@company.com", "9999999999",
                "Pune", "Engineering", "Developer", "Java", "EVT-001");

        when(eventRepository.findByEventCode("EVT-001")).thenReturn(Optional.of(event));
        when(volunteerRepository.findByEmployeeId("EMP020")).thenReturn(Optional.empty());
        when(volunteerMapper.toEntity(request)).thenReturn(newVolunteer);
        when(volunteerRepository.save(newVolunteer)).thenAnswer(inv -> {
            newVolunteer.setId(volunteerId);
            return newVolunteer;
        });
        when(enrollmentRepository.existsByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(false);

        VolunteerImportResponse response = volunteerService.importVolunteer(request);

        assertThat(response.volunteerId()).isEqualTo(volunteerId);
        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.alreadyEnrolled()).isFalse();
        assertThat(newVolunteer.getAvailability()).isEqualTo(VolunteerAvailability.AVAILABLE);
        verify(enrollmentRepository).save(any());
        verify(volunteerMapper, org.mockito.Mockito.never()).updateEntityFromImportRequest(any(), any());
    }

    @Test
    @DisplayName("importVolunteer updates an existing volunteer's profile and enrolls it")
    void importVolunteerUpdatesExistingAndEnrolls() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setId(eventId);
        event.setEventCode("EVT-002");

        VolunteerEntity existing = buildVolunteer("EMP001");
        existing.setId(volunteerId);

        VolunteerImportRequest request = new VolunteerImportRequest(
                "EMP001", "Updated Name", "updated@company.com", "8888888888",
                "Chennai", "Sales", "Manager", "Sales,Negotiation", "EVT-002");

        when(eventRepository.findByEventCode("EVT-002")).thenReturn(Optional.of(event));
        when(volunteerRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(existing));
        when(volunteerRepository.save(existing)).thenReturn(existing);
        when(enrollmentRepository.existsByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(false);

        VolunteerImportResponse response = volunteerService.importVolunteer(request);

        assertThat(response.volunteerId()).isEqualTo(volunteerId);
        assertThat(response.alreadyEnrolled()).isFalse();
        verify(volunteerMapper).updateEntityFromImportRequest(request, existing);
        verify(volunteerMapper, org.mockito.Mockito.never()).toEntity(any());
        verify(enrollmentRepository).save(any());
    }

    @Test
    @DisplayName("importVolunteer is idempotent: re-importing an already-enrolled volunteer does not create a duplicate enrollment")
    void importVolunteerAlreadyEnrolledIsNoOp() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setId(eventId);
        event.setEventCode("EVT-003");

        VolunteerEntity existing = buildVolunteer("EMP001");
        existing.setId(volunteerId);

        VolunteerImportRequest request = new VolunteerImportRequest(
                "EMP001", "Existing Name", "existing@company.com", null,
                null, null, null, null, "EVT-003");

        when(eventRepository.findByEventCode("EVT-003")).thenReturn(Optional.of(event));
        when(volunteerRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(existing));
        when(volunteerRepository.save(existing)).thenReturn(existing);
        when(enrollmentRepository.existsByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(true);

        VolunteerImportResponse response = volunteerService.importVolunteer(request);

        assertThat(response.alreadyEnrolled()).isTrue();
        verify(enrollmentRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("importVolunteer throws when the eventCode does not resolve to an event")
    void importVolunteerThrowsWhenEventCodeUnknown() {
        VolunteerImportRequest request = new VolunteerImportRequest(
                "EMP001", "Name", "email@company.com", null, null, null, null, null, "EVT-UNKNOWN");

        when(eventRepository.findByEventCode("EVT-UNKNOWN")).thenReturn(Optional.empty());

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> volunteerService.importVolunteer(request));
        verify(volunteerRepository, org.mockito.Mockito.never()).findByEmployeeId(any());
    }

    private VolunteerEntity buildVolunteer(String employeeId) {
        VolunteerEntity volunteer = new VolunteerEntity();
        volunteer.setEmployeeId(employeeId);
        volunteer.setFullName("Test Volunteer");
        return volunteer;
    }

    private VolunteerDto buildDto(String employeeId) {
        return new VolunteerDto(
                UUID.randomUUID(), employeeId, "Test Volunteer", "test@company.com", "9999999999",
                "Mumbai", "Engineering", "Developer", "Java", VolunteerAvailability.AVAILABLE,
                5, BigDecimal.valueOf(4.5));
    }
}
