package com.outreach.platform.common.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Consistent error response format for all platform services.
 *
 * @param timestamp   when the error occurred
 * @param status      HTTP status code
 * @param error       error type/category
 * @param message     human-readable error description
 * @param correlationId request correlation identifier for tracing
 * @param fieldErrors field-level validation errors (field name -> list of error messages)
 */
@Schema(description = "Standardized error response returned by all platform services")
public record ErrorResponse(
        @Schema(description = "Timestamp when the error occurred", example = "2024-06-15T10:30:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Error type/category", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Human-readable error description", example = "Validation failed for one or more fields")
        String message,

        @Schema(description = "Request correlation ID for distributed tracing", example = "550e8400-e29b-41d4-a716-446655440000")
        String correlationId,

        @Schema(description = "Field-level validation errors (field name to list of error messages)")
        Map<String, List<String>> fieldErrors
) {

    /**
     * Creates an ErrorResponse without field-level errors.
     */
    public static ErrorResponse of(int status, String error, String message, String correlationId) {
        return new ErrorResponse(Instant.now(), status, error, message, correlationId, Map.of());
    }

    /**
     * Creates an ErrorResponse with field-level errors.
     */
    public static ErrorResponse withFieldErrors(int status, String error, String message,
                                                 String correlationId, Map<String, List<String>> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, correlationId, fieldErrors);
    }
}
