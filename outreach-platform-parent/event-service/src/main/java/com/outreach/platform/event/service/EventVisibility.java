package com.outreach.platform.event.service;

import com.outreach.platform.common.security.CurrentUser;
import com.outreach.platform.event.repo.PocAssignmentRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Which events the caller may see: POCs only those they are assigned to, everyone else all of them. */
@Component
public class EventVisibility {

    private final PocAssignmentRepository pocAssignmentRepository;

    @Inject
    public EventVisibility(PocAssignmentRepository pocAssignmentRepository) {
        this.pocAssignmentRepository = pocAssignmentRepository;
    }

    /** The POC to limit event queries to, or null when the caller sees every event. */
    public UUID pocScope() {
        return CurrentUser.isPocOnly() ? CurrentUser.requireId() : null;
    }

    /** Whether a user is one of the event's POCs. */
    public boolean isAssigned(UUID eventId, UUID userId) {
        return pocAssignmentRepository.existsByEventIdAndUserId(eventId, userId);
    }

    /** Not found, as far as a POC is concerned, unless the event is theirs. */
    public void requireVisible(UUID eventId) {
        UUID poc = pocScope();
        if (poc != null && !isAssigned(eventId, poc)) {
            throw new EventService.EventNotFoundException(eventId);
        }
    }
}
