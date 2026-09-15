package com.outreach.platform.ingestion.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Fallback factory that throws EventServiceUnavailableException when the event-service is unreachable. */
@Component
public class EventServiceClientFallbackFactory implements FallbackFactory<EventServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(EventServiceClientFallbackFactory.class);

    @Override
    public EventServiceClient create(Throwable cause) {
        log.error("Event service unavailable during ingestion, activating fallback. Cause: {}", cause.getMessage());
        return new EventServiceClientFallback(cause);
    }

    private record EventServiceClientFallback(Throwable cause) implements EventServiceClient {

        @Override
        public EventServiceClient.EventResponse getEvent(UUID eventId) {
            throw new EventServiceUnavailableException(
                    "Event service unavailable: cannot retrieve event " + eventId, cause);
        }

        @Override
        public EventServiceClient.EventResponse createEvent(EventServiceClient.EventCreateRequest request) {
            throw new EventServiceUnavailableException(
                    "Event service unavailable: cannot create event " + request.eventCode(), cause);
        }

        @Override
        public List<EventServiceClient.EnrollmentResponse> enrollVolunteers(UUID eventId,
                                                                             EventServiceClient.VolunteerEnrollRequest request) {
            throw new EventServiceUnavailableException(
                    "Event service unavailable: cannot enroll volunteers for event " + eventId, cause);
        }

        @Override
        public EventServiceClient.VolunteerResponse getVolunteerProfile(String employeeId) {
            throw new EventServiceUnavailableException(
                    "Event service unavailable: cannot retrieve volunteer profile " + employeeId, cause);
        }

        @Override
        public EventServiceClient.VolunteerImportResponse importVolunteer(
                EventServiceClient.VolunteerImportRequest request) {
            throw new EventServiceUnavailableException(
                    "Event service unavailable: cannot import volunteer " + request.employeeId(), cause);
        }
    }
}
