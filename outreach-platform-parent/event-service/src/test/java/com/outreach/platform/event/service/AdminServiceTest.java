package com.outreach.platform.event.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.mapper.UserMapper;
import com.outreach.platform.event.model.EventStatus;
import com.outreach.platform.event.model.UserRole;
import com.outreach.platform.event.model.dto.AdminDashboardStats;
import com.outreach.platform.event.model.dto.UserCreateRequest;
import com.outreach.platform.event.model.dto.UserDto;
import com.outreach.platform.event.model.dto.UserRoleChangeRequest;
import com.outreach.platform.event.model.dto.UserStatusRequest;
import com.outreach.platform.event.model.dto.UserUpdateRequest;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.UserRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository, eventRepository, volunteerRepository, userMapper, passwordEncoder
        );
        // Production requests always have TenantContext populated by TenantContextFilter — set it
        // here too so these tests exercise the tenant-scoped findByIdAndTenantId path rather than
        // the plain-findById fallback that only real PLATFORM_ADMIN requests should take. See
        // docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        TenantContext.setCurrentTenantId(UUID.randomUUID());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("createUser should hash password and save new user")
    void createUserShouldHashPasswordAndSave() {
        UserCreateRequest request = new UserCreateRequest("newuser", "new@example.com", "Pass1234!", UserRole.PMO);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234!")).thenReturn("{bcrypt}$hashed");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(
                new UserDto(UUID.randomUUID(), "newuser", "new@example.com", UserRole.PMO, true)
        );

        UserDto result = adminService.createUser(request);

        assertThat(result.username()).isEqualTo("newuser");
        assertThat(result.role()).isEqualTo(UserRole.PMO);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("{bcrypt}$hashed");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isForcePasswordChange()).isTrue();
    }

    @Test
    @DisplayName("createUser should throw when username exists")
    void createUserShouldThrowWhenUsernameExists() {
        UserCreateRequest request = new UserCreateRequest("existing", "e@x.com", "Pass1234!", UserRole.ADMIN);
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createUser(request))
                .isInstanceOf(AdminService.UsernameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("updateUser should update fields when provided")
    void updateUserShouldUpdateProvidedFields() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setUsername("oldname");
        entity.setEmail("old@mail.com");

        when(userRepository.findByIdAndTenantId(eq(userId), any(UUID.class))).thenReturn(Optional.of(entity));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(entity);
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(
                new UserDto(userId, "newname", "new@mail.com", UserRole.POC, true)
        );

        UserUpdateRequest request = new UserUpdateRequest("newname", "new@mail.com", null);
        UserDto result = adminService.updateUser(userId, request);

        assertThat(result.username()).isEqualTo("newname");
        verify(userRepository).save(entity);
    }

    @Test
    @DisplayName("updateUser should throw when user not found")
    void updateUserShouldThrowWhenNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(eq(userId), any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUser(userId, new UserUpdateRequest("x", null, null)))
                .isInstanceOf(AdminService.UserNotFoundException.class);
    }

    @Test
    @DisplayName("enableDisableUser should disable user")
    void enableDisableUserShouldDisable() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setEnabled(true);

        when(userRepository.findByIdAndTenantId(eq(userId), any(UUID.class))).thenReturn(Optional.of(entity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(entity);
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(
                new UserDto(userId, "user", "u@e.com", UserRole.POC, false)
        );

        UserDto result = adminService.enableDisableUser(userId, new UserStatusRequest(false));
        assertThat(result.enabled()).isFalse();
    }

    @Test
    @DisplayName("enableDisableUser should unlock when re-enabling")
    void enableDisableUserShouldUnlockOnReEnable() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setEnabled(false);
        entity.setAccountLocked(true);
        entity.setFailedLoginAttempts(5);

        when(userRepository.findByIdAndTenantId(eq(userId), any(UUID.class))).thenReturn(Optional.of(entity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(entity);
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(
                new UserDto(userId, "user", "u@e.com", UserRole.POC, true)
        );

        adminService.enableDisableUser(userId, new UserStatusRequest(true));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertThat(saved.isAccountLocked()).isFalse();
        assertThat(saved.getFailedLoginAttempts()).isZero();
        assertThat(saved.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("changeRole should update user role")
    void changeRoleShouldUpdateRole() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setRole(UserRole.POC);

        when(userRepository.findByIdAndTenantId(eq(userId), any(UUID.class))).thenReturn(Optional.of(entity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(entity);
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(
                new UserDto(userId, "user", "u@e.com", UserRole.ADMIN, true)
        );

        UserDto result = adminService.changeRole(userId, new UserRoleChangeRequest(UserRole.ADMIN));
        assertThat(result.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("getDashboardStats should return aggregate counts")
    void getDashboardStatsShouldReturnCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByEnabled(true)).thenReturn(8L);
        when(userRepository.countByAccountLocked(true)).thenReturn(1L);
        when(eventRepository.count()).thenReturn(25L);
        when(eventRepository.countByStatusIn(any())).thenReturn(5L);
        when(volunteerRepository.count()).thenReturn(100L);

        AdminDashboardStats stats = adminService.getDashboardStats();

        assertThat(stats.totalUsers()).isEqualTo(10L);
        assertThat(stats.activeUsers()).isEqualTo(8L);
        assertThat(stats.lockedUsers()).isEqualTo(1L);
        assertThat(stats.totalEvents()).isEqualTo(25L);
        assertThat(stats.activeEvents()).isEqualTo(5L);
        assertThat(stats.totalVolunteers()).isEqualTo(100L);
    }
}
