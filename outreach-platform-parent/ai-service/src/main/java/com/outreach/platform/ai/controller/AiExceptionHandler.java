package com.outreach.platform.ai.controller;

import com.outreach.platform.ai.exception.AiProviderUnavailableException;
import com.outreach.platform.ai.exception.FeatureDisabledException;
import com.outreach.platform.common.error.ErrorResponse;
import com.outreach.platform.common.filter.CorrelationIdFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Exception handler mapping domain-specific and resilience exceptions to structured HTTP error responses. */
@RestControllerAdvice
public class AiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AiExceptionHandler.class);

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker is open for AI provider: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI_PROVIDER_UNAVAILABLE",
                "AI provider is temporarily unavailable. Please try again later.",
                getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(AiProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderUnavailable(AiProviderUnavailableException ex) {
        log.warn("AI provider unavailable: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI_PROVIDER_UNAVAILABLE",
                ex.getMessage(),
                getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(FeatureDisabledException.class)
    public ResponseEntity<ErrorResponse> handleFeatureDisabled(FeatureDisabledException ex) {
        log.info("AI feature disabled: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_IMPLEMENTED.value(),
                "AI_FEATURE_DISABLED",
                ex.getMessage(),
                getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception in AI service", ex);
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred in the AI service.",
                getCorrelationId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY);
        return correlationId != null ? correlationId : "unknown";
    }
}
