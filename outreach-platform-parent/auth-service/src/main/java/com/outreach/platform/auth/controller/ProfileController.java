package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.model.dto.UserAdminRequests.ChangePasswordRequest;
import com.outreach.platform.auth.model.dto.UserAdminRequests.UpdateUserRequest;
import com.outreach.platform.auth.service.ProfileService;
import com.outreach.platform.auth.service.ProfileService.Profile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own profile and password. */
@RestController
@RequestMapping("/api/auth/me")
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
@Tag(name = "Profile", description = "View and edit your own account")
public class ProfileController {

    private final ProfileService profiles;

    @Inject
    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @Operation(summary = "Your profile")
    @GetMapping
    public Profile get(@AuthenticationPrincipal Jwt jwt) {
        return profiles.get(jwt.getSubject());
    }

    @Operation(summary = "Update your display name and phone")
    @PutMapping
    public Profile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
        return profiles.update(jwt.getSubject(), request.displayName(), request.phone());
    }

    @Operation(summary = "Change your password", description = "Requires your current password")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        profiles.changePassword(jwt.getSubject(), request.currentPassword(), request.newPassword(), request.otpCode());
        return ResponseEntity.noContent().build();
    }
}
