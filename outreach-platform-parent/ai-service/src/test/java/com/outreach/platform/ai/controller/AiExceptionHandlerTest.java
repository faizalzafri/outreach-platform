package com.outreach.platform.ai.controller;

import com.outreach.platform.ai.exception.AiProviderUnavailableException;
import com.outreach.platform.ai.exception.FeatureDisabledException;
import com.outreach.platform.common.error.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AiExceptionHandler.
 * Verifies correct HTTP status codes and error types for AI-specific exceptions.
 */
class AiExceptionHandlerTest {

    private AiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AiExceptionHandler();
    }

    @Test
    void handleCircuitBreakerOpen_returns503WithCorrectErrorType() {
        CallNotPermittedException ex = mock(CallNotPermittedException.class);
        when(ex.getMessage()).thenReturn("CircuitBreaker 'aiProvider' is OPEN");

        ResponseEntity<ErrorResponse> response = handler.handleCircuitBreakerOpen(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().error()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
    }

    @Test
    void handleAiProviderUnavailable_returns503() {
        AiProviderUnavailableException ex = new AiProviderUnavailableException(
                "AI provider is currently unavailable. Operation: summarize"
        );

        ResponseEntity<ErrorResponse> response = handler.handleAiProviderUnavailable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().error()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
        assertThat(response.getBody().message()).contains("summarize");
    }

    @Test
    void handleFeatureDisabled_returns501() {
        FeatureDisabledException ex = new FeatureDisabledException("summarize");

        ResponseEntity<ErrorResponse> response = handler.handleFeatureDisabled(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(501);
        assertThat(response.getBody().error()).isEqualTo("AI_FEATURE_DISABLED");
        assertThat(response.getBody().message()).contains("summarize");
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
    }
}
