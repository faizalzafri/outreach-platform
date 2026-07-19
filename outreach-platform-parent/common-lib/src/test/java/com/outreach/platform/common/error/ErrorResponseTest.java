package com.outreach.platform.common.error;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void of_createsResponseWithoutFieldErrors() {
        ErrorResponse response = ErrorResponse.of(404, "NOT_FOUND", "Resource not found", "abc-123");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("NOT_FOUND");
        assertThat(response.message()).isEqualTo("Resource not found");
        assertThat(response.correlationId()).isEqualTo("abc-123");
        assertThat(response.fieldErrors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void withFieldErrors_createsResponseWithFieldErrors() {
        Map<String, List<String>> fieldErrors = Map.of(
                "email", List.of("must not be blank"),
                "name", List.of("must not be null", "must be at least 2 characters")
        );

        ErrorResponse response = ErrorResponse.withFieldErrors(
                400, "VALIDATION_ERROR", "Validation failed", "xyz-456", fieldErrors);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.fieldErrors()).containsKey("email");
        assertThat(response.fieldErrors().get("name")).hasSize(2);
    }
}
