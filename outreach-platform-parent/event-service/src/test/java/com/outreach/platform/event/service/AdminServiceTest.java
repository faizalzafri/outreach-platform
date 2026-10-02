package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.mapper.UserMapper;
import com.outreach.platform.event.model.UserRole;
import com.outreach.platform.event.model.dto.AdminDashboardStats;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.UserRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private UserMapper userMapper;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, eventRepository, volunteerRepository, userMapper);
    }

    @Test
    @DisplayName("listUsers accepts the role in either form the UI sends (POC or ROLE_POC)")
    void listUsersByRole() {
        var page = PageRequest.of(0, 20);
        when(userRepository.findByRole(eq(UserRole.POC), any())).thenReturn(new PageImpl<>(List.of(new UserEntity())));

        assertThat(adminService.listUsers(page, "POC").getTotalElements()).isEqualTo(1);
        assertThat(adminService.listUsers(page, "ROLE_POC").getTotalElements()).isEqualTo(1);
        verify(userRepository, org.mockito.Mockito.times(2)).findByRole(UserRole.POC, page);
    }

    @Test
    @DisplayName("getDashboardStats should return aggregate counts")
    void getDashboardStatsShouldReturnCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByEnabled(true)).thenReturn(8L);
        when(eventRepository.count()).thenReturn(25L);
        when(eventRepository.countByStatusIn(any())).thenReturn(5L);
        when(volunteerRepository.count()).thenReturn(100L);

        AdminDashboardStats stats = adminService.getDashboardStats();

        assertThat(stats.totalUsers()).isEqualTo(10L);
        assertThat(stats.activeUsers()).isEqualTo(8L);
        assertThat(stats.totalEvents()).isEqualTo(25L);
        assertThat(stats.activeEvents()).isEqualTo(5L);
        assertThat(stats.totalVolunteers()).isEqualTo(100L);
    }
}
