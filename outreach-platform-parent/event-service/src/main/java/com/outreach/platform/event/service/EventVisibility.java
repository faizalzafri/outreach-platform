package com.outreach.platform.event.service;

import com.outreach.platform.common.security.CurrentUser;
import com.outreach.platform.event.model.AccessLevel;
import com.outreach.platform.event.repo.EventTeamAccessRepository;
import com.outreach.platform.event.repo.PocAssignmentRepository;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Which events the caller may see: POCs only those they are assigned to, directly or through a
 * team the event is shared with; everyone else all of them.
 */
@Component
public class EventVisibility {

    private final PocAssignmentRepository pocAssignmentRepository;
    private final EventTeamAccessRepository teamAccessRepository;

    @Inject
    public EventVisibility(PocAssignmentRepository pocAssignmentRepository,
                           EventTeamAccessRepository teamAccessRepository) {
        this.pocAssignmentRepository = pocAssignmentRepository;
        this.teamAccessRepository = teamAccessRepository;
    }

    /** The POC to limit event queries to, or null when the caller sees every event. */
    public UUID pocScope() {
        return CurrentUser.isPocOnly() ? CurrentUser.requireId() : null;
    }

    /** Whether a user may work on the event: one of its POCs, or in a team it is shared with. */
    public boolean isAssigned(UUID eventId, UUID userId) {
        return pocAssignmentRepository.existsByEventIdAndUserId(eventId, userId)
                || teamAccessRepository.existsForMember(eventId, userId, Set.of(AccessLevel.VIEW, AccessLevel.EDIT));
    }

    /** Whether a user may record the event's attendance: one of its POCs, or in a team with edit access. */
    public boolean canEdit(UUID eventId, UUID userId) {
        return pocAssignmentRepository.existsByEventIdAndUserId(eventId, userId)
                || teamAccessRepository.existsForMember(eventId, userId, Set.of(AccessLevel.EDIT));
    }

    /** Not found, as far as a POC is concerned, unless the event is theirs. */
    public void requireVisible(UUID eventId) {
        UUID poc = pocScope();
        if (poc != null && !isAssigned(eventId, poc)) {
            throw new EventService.EventNotFoundException(eventId);
        }
    }
}
