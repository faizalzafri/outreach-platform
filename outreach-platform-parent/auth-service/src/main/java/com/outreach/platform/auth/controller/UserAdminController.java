package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.model.dto.UserAdminRequests.ChangeRoleRequest;
import com.outreach.platform.auth.model.dto.UserAdminRequests.InviteUserRequest;
import com.outreach.platform.auth.model.dto.UserAdminRequests.UpdateUserRequest;
import com.outreach.platform.auth.service.AccountAdminService;
import com.outreach.platform.auth.service.AccountAdminService.InviteCommand;
import com.outreach.platform.auth.service.AccountAdminService.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * User administration for tenant admins (their own organization) and platform admins (any
 * organization, named by {@code tenantId}). Users are invited by email and set their own password.
 */
@RestController
@RequestMapping("/api/auth/users")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM_ADMIN')")
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
@Tag(name = "User Administration", description = "Invite and manage users within an organization")
public class UserAdminController {

    private final AccountAdminService admin;

    @Inject
    public UserAdminController(AccountAdminService admin) {
        this.admin = admin;
    }

    @Operation(summary = "List the organization's users")
    @GetMapping
    public List<UserSummary> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID tenantId) {
        return admin.list(Caller.of(jwt).tenantFor(tenantId));
    }

    @Operation(summary = "Get one user")
    @GetMapping("/{userId}")
    public UserSummary get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                           @RequestParam(required = false) UUID tenantId) {
        return admin.get(Caller.of(jwt).tenantFor(tenantId), userId);
    }

    @Operation(summary = "Invite a user", description = "Creates the account and emails a single-use activation link")
    @PostMapping
    public ResponseEntity<UserSummary> invite(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam(required = false) UUID tenantId,
                                              @Valid @RequestBody InviteUserRequest request) {
        Caller caller = Caller.of(jwt);
        UserSummary created = admin.invite(caller.tenantFor(tenantId),
                new InviteCommand(request.username(), request.email(), request.displayName(), request.role()),
                caller.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Edit a user's name and phone")
    @PatchMapping("/{userId}")
    public UserSummary update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                              @RequestParam(required = false) UUID tenantId,
                              @Valid @RequestBody UpdateUserRequest request) {
        return admin.updateProfile(Caller.of(jwt).tenantFor(tenantId), userId, request.displayName(), request.phone());
    }

    @Operation(summary = "Change a user's role")
    @PutMapping("/{userId}/role")
    public UserSummary changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                  @RequestParam(required = false) UUID tenantId,
                                  @Valid @RequestBody ChangeRoleRequest request) {
        Caller caller = Caller.of(jwt);
        return admin.changeRole(caller.tenantFor(tenantId), userId, request.role(), caller.userId());
    }

    @Operation(summary = "Disable a user", description = "They can no longer sign in; their data is kept")
    @PostMapping("/{userId}/disable")
    public UserSummary disable(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                               @RequestParam(required = false) UUID tenantId) {
        Caller caller = Caller.of(jwt);
        return admin.setEnabled(caller.tenantFor(tenantId), userId, false, caller.userId());
    }

    @Operation(summary = "Enable a user", description = "Also clears a sign-in lockout")
    @PostMapping("/{userId}/enable")
    public UserSummary enable(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                              @RequestParam(required = false) UUID tenantId) {
        Caller caller = Caller.of(jwt);
        return admin.setEnabled(caller.tenantFor(tenantId), userId, true, caller.userId());
    }

    @Operation(summary = "Resend the invitation email", description = "Earlier links stop working")
    @PostMapping("/{userId}/invitation")
    public ResponseEntity<Void> resendInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                                 @RequestParam(required = false) UUID tenantId) {
        Caller caller = Caller.of(jwt);
        admin.resendInvitation(caller.tenantFor(tenantId), userId, caller.username());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Revoke a pending invitation")
    @DeleteMapping("/{userId}/invitation")
    public ResponseEntity<Void> revokeInvitation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                                 @RequestParam(required = false) UUID tenantId) {
        admin.revokeInvitation(Caller.of(jwt).tenantFor(tenantId), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Email the user a password reset link")
    @PostMapping("/{userId}/password-reset")
    public ResponseEntity<Void> sendPasswordReset(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                                  @RequestParam(required = false) UUID tenantId) {
        admin.sendPasswordReset(Caller.of(jwt).tenantFor(tenantId), userId);
        return ResponseEntity.accepted().build();
    }
}
