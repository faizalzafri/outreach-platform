package com.outreach.platform.event.service;

import com.outreach.platform.event.mapper.UserMapper;
import com.outreach.platform.event.model.UserRole;
import com.outreach.platform.event.model.dto.AdminDashboardStats;
import com.outreach.platform.event.model.dto.UserDto;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.UserRepository;
import com.outreach.platform.event.repo.VolunteerRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin read models: the user directory (for pickers such as POC assignment) and dashboard
 * statistics. Users are created and managed in auth-service; this service only reads its copy.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final VolunteerRepository volunteerRepository;
    private final UserMapper userMapper;

    @Inject
    public AdminService(UserRepository userRepository,
                        EventRepository eventRepository,
                        VolunteerRepository volunteerRepository,
                        UserMapper userMapper) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.volunteerRepository = volunteerRepository;
        this.userMapper = userMapper;
    }

    /**
     * Lists users with pagination, optionally only those with one role.
     */
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable, String role) {
        if (role == null || role.isBlank()) {
            return userRepository.findAll(pageable).map(userMapper::toDto);
        }
        UserRole userRole = UserRole.valueOf(role.toUpperCase().replace("ROLE_", ""));
        return userRepository.findByRole(userRole, pageable).map(userMapper::toDto);
    }

    /**
     * Returns aggregate dashboard statistics for the admin panel.
     */
    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabled(true);
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
                totalEvents,
                activeEvents,
                totalVolunteers
        );
    }
}
