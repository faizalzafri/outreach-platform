package com.outreach.platform.auth.model.dto;

import com.outreach.platform.auth.model.TenantRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request bodies for {@code /api/auth/users} and {@code /api/auth/me}. */
public final class UserAdminRequests {

    private UserAdminRequests() {
    }

    public record InviteUserRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
                    message = "3–50 letters, digits, dots, hyphens or underscores") String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String displayName,
            @NotNull TenantRole role) {
    }

    public record UpdateUserRequest(
            @Size(max = 100) String displayName,
            @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,20}$", message = "must be a phone number") String phone) {
    }

    public record ChangeRoleRequest(@NotNull TenantRole role) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword,
            /** Required only when the caller's policy asks for a one-time code for password changes. */
            String otpCode) {
    }
}
