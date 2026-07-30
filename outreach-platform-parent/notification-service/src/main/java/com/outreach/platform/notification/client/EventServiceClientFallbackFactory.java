package com.outreach.platform.notification.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback factory for EventServiceClient that returns defaults when event-service is unavailable.
 */
@Component
public class EventServiceClientFallbackFactory implements FallbackFactory<EventServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(EventServiceClientFallbackFactory.class);

    @Override
    public EventServiceClient create(Throwable cause) {
        log.warn("Event service unavailable, activating fallback. Cause: {}", cause.getMessage());
        return new EventServiceClientFallback(cause);
    }

    private record EventServiceClientFallback(Throwable cause) implements EventServiceClient {

        @Override
        public EventResponse getEvent(UUID eventId) {
            log.warn("Fallback: getEvent({}) - returning null. Cause: {}", eventId, cause.getMessage());
            return null;
        }

        @Override
        public List<EnrollmentResponse> getEventVolunteers(UUID eventId) {
            log.warn("Fallback: getEventVolunteers({}) - returning empty list. Cause: {}", eventId, cause.getMessage());
            return Collections.emptyList();
        }

        @Override
        public VolunteerResponse getVolunteerProfile(String employeeId) {
            log.warn("Fallback: getVolunteerProfile({}) - returning null. Cause: {}", employeeId, cause.getMessage());
            return null;
        }
    }
}
