package com.outreach.platform.feedback.repo;

import com.outreach.platform.feedback.entity.VolunteerFeedbackEntity;
import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for volunteer feedback entities.
 * Provides standard CRUD operations plus custom search and stats queries.
 */
@Repository
public interface VolunteerFeedbackRepository extends JpaRepository<VolunteerFeedbackEntity, UUID> {

    Page<VolunteerFeedbackEntity> findByEventId(UUID eventId, Pageable pageable);

    List<VolunteerFeedbackEntity> findByVolunteerId(UUID volunteerId);

    Optional<VolunteerFeedbackEntity> findByEventIdAndVolunteerId(UUID eventId, UUID volunteerId);

    /**
     * Search feedback with dynamic filter criteria. Null parameters are treated as "no filter".
     */
    @Query("""
            SELECT f FROM VolunteerFeedbackEntity f
            WHERE (:eventId IS NULL OR f.eventId = :eventId)
              AND (:volunteerId IS NULL OR f.volunteerId = :volunteerId)
              AND (:category IS NULL OR f.category = :category)
              AND (:sentiment IS NULL OR f.sentiment = :sentiment)
              AND (:status IS NULL OR f.status = :status)
              AND (:minScore IS NULL OR f.score >= :minScore)
              AND (:maxScore IS NULL OR f.score <= :maxScore)
              AND (CAST(:dateFrom AS timestamp) IS NULL OR f.submittedAt >= :dateFrom)
              AND (CAST(:dateTo AS timestamp) IS NULL OR f.submittedAt <= :dateTo)
            """)
    Page<VolunteerFeedbackEntity> search(
            @Param("eventId") UUID eventId,
            @Param("volunteerId") UUID volunteerId,
            @Param("category") String category,
            @Param("sentiment") FeedbackSentiment sentiment,
            @Param("status") FeedbackStatus status,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable
    );

    /**
     * Count submitted feedback for a given event.
     */
    long countByEventId(UUID eventId);

    /**
     * Count feedback by event and status.
     */
    long countByEventIdAndStatus(UUID eventId, FeedbackStatus status);

    /**
     * Calculate average score for a given event.
     */
    @Query("SELECT COALESCE(AVG(f.score), 0.0) FROM VolunteerFeedbackEntity f WHERE f.eventId = :eventId")
    double averageScoreByEventId(@Param("eventId") UUID eventId);
}
