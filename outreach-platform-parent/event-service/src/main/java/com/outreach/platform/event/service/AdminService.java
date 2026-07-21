package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.mapper.UserMapper;
import com.outreach.platform.event.model.dto.AdminDashboardStats;
import com.outreach.platform.event.model.dto.UserCreateRequest;
import com.outreach.platform.event.model.dto.UserDto;
import com.outreach.platform.event.model.dto.UserRoleChangeRequest;
import com.outreach.platform.event.model.dto.UserStatusRequest;
import com.outreach.platform.event.model.dto.UserUpdateRequest;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.UserRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for admin user management and dashboard operations.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public AdminService(UserRepository userRepository,
                        EventRepository eventRepository,
                        VolunteerRepository volunteerRepository,
                        UserMapper userMapper,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.volunteerRepository = volunteerRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lists all users with pagination.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    /**
     * Creates a new user account.
     *
     * @throws UsernameAlreadyExistsException if username is taken
     */
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(request.username());
        entity.setEmail(request.email());
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setRole(request.role());
        entity.setEnabled(true);
        entity.setAccountLocked(false);
        entity.setFailedLoginAttempts(0);
        entity.setForcePasswordChange(true);

        UserEntity saved = userRepository.save(entity);
        return userMapper.toDto(saved);
    }

    /**
     * Updates an existing user's profile information.
     *
     * @throws UserNotFoundException if user not found
     * @throws UsernameAlreadyExistsException if new username is taken by another user
     */
    @Transactional
    public UserDto updateUser(UUID userId, UserUpdateRequest request) {
        UserEntity entity = findUserOrThrow(userId);

        if (request.username() != null && !request.username().isBlank()) {
            if (!entity.getUsername().equals(request.username()) &&
                    userRepository.existsByUsername(request.username())) {
                throw new UsernameAlreadyExistsException(request.username());
            }
            entity.setUsername(request.username());
        }
        if (request.email() != null && !request.email().isBlank()) {
            entity.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        UserEntity saved = userRepository.save(entity);
        return userMapper.toDto(saved);
    }

    /**
     * Enables or disables a user account.
     *
     * @throws UserNotFoundException if user not found
     */
    @Transactional
    public UserDto enableDisableUser(UUID userId, UserStatusRequest request) {
        UserEntity entity = findUserOrThrow(userId);
        entity.setEnabled(request.enabled());

        if (request.enabled()) {
            entity.setAccountLocked(false);
            entity.setFailedLoginAttempts(0);
            entity.setLockedUntil(null);
        }

        UserEntity saved = userRepository.save(entity);
        return userMapper.toDto(saved);
    }

    /**
     * Changes a user's role.
     *
     * @throws UserNotFoundException if user not found
     * @throws InvalidRoleException if the role value is invalid
     */
    @Transactional
    public UserDto changeRole(UUID userId, UserRoleChangeRequest request) {
        UserEntity entity = findUserOrThrow(userId);

        if (request.role() == null) {
            throw new InvalidRoleException("Role cannot be null");
        }

        entity.setRole(request.role());
        UserEntity saved = userRepository.save(entity);
        return userMapper.toDto(saved);
    }

    /**
     * Returns aggregate dashboard statistics for the admin panel.
     */
    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabled(true);
        long lockedUsers = userRepository.countByAccountLocked(true);
        long totalEvents = eventRepository.count();
        long activeEvents = eventRepository.countByStatusIn(
                java.util.List.of(
                        com.outreach.platform.event.model.EventStatus.PUBLISHED,
                        com.outreach.platform.event.model.EventStatus.ACTIVE
                )
        );
        long totalVolunteers = volunteerRepository.count();

        return new AdminDashboardStats(
                totalUsers,
                activeUsers,
                lockedUsers,
                totalEvents,
                activeEvents,
                totalVolunteers
        );
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Thrown when a user is not found by ID.
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID userId) {
            super("User not found: " + userId);
        }
    }

    /**
     * Thrown when a username is already taken.
     */
    public static class UsernameAlreadyExistsException extends RuntimeException {
        public UsernameAlreadyExistsException(String username) {
            super("Username already exists: " + username);
        }
    }

    /**
     * Thrown when an invalid role is specified.
     */
    public static class InvalidRoleException extends RuntimeException {
        public InvalidRoleException(String message) {
            super(message);
        }
    }
}
