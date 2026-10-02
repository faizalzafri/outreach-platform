package com.outreach.platform.event.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.EventTeamAccess;
import com.outreach.platform.event.entity.Team;
import com.outreach.platform.event.model.AccessLevel;
import com.outreach.platform.event.model.dto.TeamAccessDto;
import com.outreach.platform.event.repo.EventRepository;
import com.outreach.platform.event.repo.EventTeamAccessRepository;
import com.outreach.platform.event.repo.TeamRepository;
import jakarta.inject.Inject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Sharing an event with teams, whose POCs then work on it as if assigned. */
@Service
public class EventTeamAccessService {

    private final EventTeamAccessRepository accessRepository;
    private final EventRepository eventRepository;
    private final TeamRepository teamRepository;

    @Inject
    public EventTeamAccessService(EventTeamAccessRepository accessRepository, EventRepository eventRepository,
                                  TeamRepository teamRepository) {
        this.accessRepository = accessRepository;
        this.eventRepository = eventRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamAccessDto> list(UUID eventId) {
        requireEvent(eventId);
        return accessRepository.findByEventId(eventId).stream()
                .map(access -> new TeamAccessDto(access.getTeamId(), requireTeam(access.getTeamId()).getName(),
                        access.getAccessLevel()))
                .toList();
    }

    /** Gives a team access to an event, or changes the access it has. */
    @Transactional
    public List<TeamAccessDto> grant(UUID eventId, UUID teamId, AccessLevel level) {
        requireEvent(eventId);
        requireTeam(teamId);
        EventTeamAccess access = accessRepository.findByEventIdAndTeamId(eventId, teamId).orElseGet(() -> {
            EventTeamAccess created = new EventTeamAccess();
            created.setEventId(eventId);
            created.setTeamId(teamId);
            return created;
        });
        access.setAccessLevel(level);
        accessRepository.save(access);
        return list(eventId);
    }

    @Transactional
    public void revoke(UUID eventId, UUID teamId) {
        requireEvent(eventId);
        accessRepository.findByEventIdAndTeamId(eventId, teamId).ifPresent(accessRepository::delete);
    }

    // Sharing works within one tenant, so a platform admin must be acting in a tenant.
    private void requireEvent(UUID eventId) {
        eventRepository.findByIdAndTenantId(eventId, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new EventService.EventNotFoundException(eventId));
    }

    private Team requireTeam(UUID teamId) {
        return teamRepository.findByIdAndTenantId(teamId, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found: " + teamId));
    }
}
