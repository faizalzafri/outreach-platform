package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.AdminDashboardStats;
import com.outreach.platform.event.model.dto.AuditLogEntry;
import com.outreach.platform.event.model.dto.AuditLogSearchCriteria;
import com.outreach.platform.event.model.dto.SystemConfigDto;
import com.outreach.platform.event.model.dto.UserCreateRequest;
import com.outreach.platform.event.model.dto.UserDto;
import com.outreach.platform.event.model.dto.UserRoleChangeRequest;
import com.outreach.platform.event.model.dto.UserStatusRequest;
import com.outreach.platform.event.model.dto.UserUpdateRequest;
import com.outreach.platform.event.service.AdminService;
import com.outreach.platform.event.service.AuditLogService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for admin operations: user management, audit log, system config, and dashboard.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @Inject
    public AdminController(AdminService adminService, AuditLogService auditLogService) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
    }

    // ─── User Management ────────────────────────────────────────────────────────

    /**
     * List all users with pagination.
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> listUsers(Pageable pageable) {
        Page<UserDto> users = adminService.listUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Create a new user account.
     */
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDto created = adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing user.
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id,
                                              @Valid @RequestBody UserUpdateRequest request) {
        UserDto updated = adminService.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Enable or disable a user account.
     */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserDto> changeUserStatus(@PathVariable UUID id,
                                                    @Valid @RequestBody UserStatusRequest request) {
        UserDto updated = adminService.enableDisableUser(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Change a user's role.
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserDto> changeUserRole(@PathVariable UUID id,
                                                  @Valid @RequestBody UserRoleChangeRequest request) {
        UserDto updated = adminService.changeRole(id, request);
        return ResponseEntity.ok(updated);
    }

    // ─── Audit Log ──────────────────────────────────────────────────────────────

    /**
     * Query audit log entries with optional filters and pagination.
     */
    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLogEntry>> queryAuditLog(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            Pageable pageable) {

        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(
                userId, action, resourceType, dateFrom, dateTo
        );
        Page<AuditLogEntry> page = auditLogService.queryAuditLogs(criteria, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Export audit log entries as CSV.
     */
    @GetMapping("/audit-log/export")
    public ResponseEntity<byte[]> exportAuditLog(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo) {

        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(
                userId, action, resourceType, dateFrom, dateTo
        );
        String csv = auditLogService.exportAuditLogCsv(criteria);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log-export.csv");

        return ResponseEntity.ok().headers(headers).body(csv.getBytes());
    }

    // ─── System Configuration ───────────────────────────────────────────────────

    /**
     * View current system configuration.
     */
    @GetMapping("/system/config")
    public ResponseEntity<SystemConfigDto> getSystemConfig() {
        SystemConfigDto config = new SystemConfigDto(
                50,
                20,
                "EVT",
                Map.of(
                        "server.port", "9004",
                        "spring.datasource.url", "jdbc:postgresql://localhost:5432/outreachfeedbackdb",
                        "spring.data.mongodb.uri", "mongodb://localhost:27017/outreach_nosql"
                )
        );
        return ResponseEntity.ok(config);
    }

    /**
     * Update system configuration (returns updated view).
     */
    @PutMapping("/system/config")
    public ResponseEntity<SystemConfigDto> updateSystemConfig(@RequestBody SystemConfigDto config) {
        // Configuration updates would be persisted to a config store in a full implementation.
        // For now, acknowledge the update and return the submitted config.
        return ResponseEntity.ok(config);
    }

    // ─── Dashboard ──────────────────────────────────────────────────────────────

    /**
     * Admin dashboard statistics: user, event, and volunteer counts.
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStats> dashboardStats() {
        AdminDashboardStats stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
