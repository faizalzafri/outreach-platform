package com.outreach.platform.ai.exception;

/**
 * Thrown when the AI provider is unavailable (circuit breaker open, timeout, or connection failure).
 */
public class AiProviderUnavailableException extends RuntimeException {

    public AiProviderUnavailableException(String message) {
        super(message);
    }

    public AiProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
