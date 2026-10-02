package com.outreach.platform.feedback.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * What feedback-service asks event-service: names for the ids it stores, and whether feedback
 * may be given for an event. Authenticated with the service's client-credentials token and the
 * caller's tenant (common-lib's FeignAuthAutoConfiguration).
 */
@FeignClient(name = "event-service")
public interface EventServiceClient {

    @PostMapping("/events/names")
    Map<UUID, String> eventNames(@RequestBody Collection<UUID> eventIds);

    @PostMapping("/volunteers/names")
    Map<UUID, String> volunteerNames(@RequestBody Collection<UUID> volunteerIds);

    @GetMapping("/events/{eventId}/feedback-eligibility")
    FeedbackEligibility feedbackEligibility(@PathVariable("eventId") UUID eventId,
                                            @RequestParam(value = "userId", required = false) UUID userId);

    /** The event's lifecycle status and whether the asked-about user is one of its POCs. */
    record FeedbackEligibility(UUID eventId, String status, boolean assigned) {
    }
}
