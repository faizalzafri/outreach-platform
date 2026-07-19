package com.outreach.platform.common.error;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Consistent error response format for all platform services.
 * Contains timestamp, HTTP status, error type, message, correlation ID, and field-level errors.
 *
 * @param timestamp   when the error occurred
 * @param status      HTTP status code
 * @param error       error type/category
 * @param message     human-readable error description
 * @param correlationId request correlation identifier for tracing
 * @param fieldErrors field-level validation errors (field name -> list of error messages)
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String correlationId,
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
