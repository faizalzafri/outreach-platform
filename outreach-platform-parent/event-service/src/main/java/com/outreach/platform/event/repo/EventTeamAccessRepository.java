package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.EventTeamAccess;
import com.outreach.platform.event.model.AccessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Teams given access to events. */
@Repository
public interface EventTeamAccessRepository extends JpaRepository<EventTeamAccess, UUID> {

    List<EventTeamAccess> findByEventId(UUID eventId);

    Optional<EventTeamAccess> findByEventIdAndTeamId(UUID eventId, UUID teamId);

    void deleteByTeamId(UUID teamId);

    /** Whether the user is in a team given at least one of these levels on the event. */
    @Query("SELECT COUNT(a) > 0 FROM EventTeamAccess a, TeamMembership m "
            + "WHERE a.eventId = :eventId AND m.teamId = a.teamId AND m.userId = :userId "
            + "AND a.accessLevel IN :levels")
    boolean existsForMember(@Param("eventId") UUID eventId, @Param("userId") UUID userId,
                            @Param("levels") Collection<AccessLevel> levels);
}
