package com.outreach.platform.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when the AI provider is unavailable (circuit breaker open, timeout, or connection failure). */
public class AiProviderUnavailableException extends ResponseStatusException {

    public AiProviderUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public AiProviderUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
