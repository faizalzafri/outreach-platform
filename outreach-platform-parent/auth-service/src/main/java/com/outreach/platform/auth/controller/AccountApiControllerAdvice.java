package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.service.AccountAdminService.AccountConflictException;
import com.outreach.platform.auth.service.AccountAdminService.AccountNotFoundException;
import com.outreach.platform.auth.service.PasswordService.PasswordPolicyViolationException;
import com.outreach.platform.common.error.ErrorResponse;
import com.outreach.platform.common.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Maps account errors from the user-admin and profile APIs to the platform's shared error shape. */
@RestControllerAdvice(assignableTypes = {UserAdminController.class, ProfileController.class, SecurityPolicyController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountApiControllerAdvice {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(AccountNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage());
    }

    @ExceptionHandler({AccountConflictException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> conflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, "USER_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(com.outreach.platform.auth.service.ProfileService.WrongPasswordException.class)
    public ResponseEntity<ErrorResponse> wrongPassword(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(), "WRONG_PASSWORD", ex.getMessage(),
                correlationId(), Map.of("currentPassword", java.util.List.of(ex.getMessage()))));
    }

    /** The UI recognizes this code, shows a code field, and resubmits with the code. */
    @ExceptionHandler(com.outreach.platform.auth.service.ProfileService.OtpRequiredException.class)
    public ResponseEntity<ErrorResponse> otpRequired(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, "OTP_REQUIRED", ex.getMessage());
    }

    @ExceptionHandler(com.outreach.platform.auth.service.ProfileService.OtpRejectedException.class)
    public ResponseEntity<ErrorResponse> otpRejected(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(), "OTP_REJECTED", ex.getMessage(),
                correlationId(), Map.of("otpCode", java.util.List.of(ex.getMessage()))));
    }

    @ExceptionHandler(com.outreach.platform.auth.service.otp.OtpService.OtpCooldownException.class)
    public ResponseEntity<ErrorResponse> otpCooldown(com.outreach.platform.auth.service.otp.OtpService.OtpCooldownException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.retryAfterSeconds()))
                .body(ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS.value(), "OTP_COOLDOWN", ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    public ResponseEntity<ErrorResponse> weakPassword(PasswordPolicyViolationException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(), "PASSWORD_POLICY", "The new password does not meet the password policy",
                correlationId(), Map.of("newPassword", ex.violations())));
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), code, message, correlationId()));
    }

    private static String correlationId() {
        String id = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        return id != null ? id : "unknown";
    }
}
