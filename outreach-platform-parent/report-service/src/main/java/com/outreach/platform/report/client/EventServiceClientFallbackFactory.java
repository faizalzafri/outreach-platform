package com.outreach.platform.report.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback factory for EventServiceClient in Report Service.
 * Returns empty/default results when event-service is unavailable.
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
        public EventServiceClient.EventResponse getEvent(UUID eventId) {
            log.warn("Fallback: getEvent({}) - returning null. Cause: {}", eventId, cause.getMessage());
            return null;
        }

        @Override
        public EventServiceClient.PagedEventResponse listEvents(int page, int size) {
            log.warn("Fallback: listEvents(page={}, size={}) - returning empty page. Cause: {}", page, size, cause.getMessage());
            return new EventServiceClient.PagedEventResponse(Collections.emptyList(), 0, 0, page, size);
        }

        @Override
        public List<EventServiceClient.EnrollmentResponse> getEventVolunteers(UUID eventId) {
            log.warn("Fallback: getEventVolunteers({}) - returning empty list. Cause: {}", eventId, cause.getMessage());
            return Collections.emptyList();
        }
    }
}
