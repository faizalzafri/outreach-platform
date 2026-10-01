package com.outreach.platform.ingestion.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Fallback factory that throws EventServiceUnavailableException when the event-service is unreachable. */
@Component
public class EventServiceClientFallbackFactory implements FallbackFactory<EventServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(EventServiceClientFallbackFactory.class);

    @Override
    public EventServiceClient create(Throwable cause) {
        log.error("Event service unavailable during ingestion, activating fallback. Cause: {}", cause.getMessage());
        return request -> {
            throw new EventServiceUnavailableException(
                    "Event service unavailable: cannot import volunteer " + request.employeeId(), cause);
        };
    }
}
