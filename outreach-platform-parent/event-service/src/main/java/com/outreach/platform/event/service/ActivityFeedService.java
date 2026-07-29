package com.outreach.platform.event.service;

import com.outreach.platform.event.model.ActivityEvent;
import com.outreach.platform.event.model.dto.ActivityFeedFilter;
import com.outreach.platform.event.model.dto.ActivityFeedResponse;
import com.outreach.platform.event.repo.ActivityEventRepository;
import jakarta.inject.Inject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for recording activity events and querying the activity feed.
 * Activities are stored in MongoDB as append-only documents with cursor-based pagination.
 */
@Service
public class ActivityFeedService {

    private final MongoTemplate mongoTemplate;
    private final ActivityEventRepository activityEventRepository;

    @Inject
    public ActivityFeedService(MongoTemplate mongoTemplate, ActivityEventRepository activityEventRepository) {
        this.mongoTemplate = mongoTemplate;
        this.activityEventRepository = activityEventRepository;
    }

    /**
     * Records an activity event to MongoDB (fire-and-forget, non-blocking).
     *
     * @param event the activity event to persist
     */
    @Async
    public void record(ActivityEvent event) {
        activityEventRepository.save(event);
    }

    /**
     * Retrieves the activity feed for a user within a tenant, applying optional filters
     * and cursor-based pagination.
     *
     * @param tenantId the tenant ID to scope the query
     * @param userId   the requesting user's ID (for visibility filtering)
     * @param filter   optional filter criteria (teamId, actionType, resourceType, dateFrom, dateTo)
     * @param cursor   optional cursor (timestamp) for fetching the next page (events before this timestamp)
     * @param limit    maximum number of items to return
     * @return the activity feed response with items and a next cursor
     */
    public ActivityFeedResponse getFeed(UUID tenantId, UUID userId, ActivityFeedFilter filter, Instant cursor, int limit) {
        Query query = new Query();

        // Tenant scoping
        query.addCriteria(Criteria.where("tenantId").is(tenantId));

        // Cursor-based pagination: fetch events before the cursor timestamp
        if (cursor != null) {
            query.addCriteria(Criteria.where("timestamp").lt(cursor));
        }

        // Apply optional filters
        if (filter.teamId() != null) {
            query.addCriteria(Criteria.where("teamId").is(filter.teamId()));
        }
        if (filter.actionType() != null) {
            query.addCriteria(Criteria.where("actionType").is(filter.actionType()));
        }
        if (filter.resourceType() != null) {
            query.addCriteria(Criteria.where("resourceType").is(filter.resourceType()));
        }
        if (filter.dateFrom() != null && filter.dateTo() != null) {
            query.addCriteria(Criteria.where("timestamp").gte(filter.dateFrom()).lte(filter.dateTo()));
        } else if (filter.dateFrom() != null) {
            query.addCriteria(Criteria.where("timestamp").gte(filter.dateFrom()));
        } else if (filter.dateTo() != null) {
            query.addCriteria(Criteria.where("timestamp").lte(filter.dateTo()));
        }

        // Sort by timestamp descending (newest first) and limit results
        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        query.limit(limit);

        List<ActivityEvent> items = mongoTemplate.find(query, ActivityEvent.class);

        // Determine next cursor: timestamp of the last item in the result, or null if fewer items than limit
        Instant nextCursor = null;
        if (items.size() == limit) {
            ActivityEvent lastItem = items.get(items.size() - 1);
            nextCursor = lastItem.getTimestamp();
        }

        return new ActivityFeedResponse(items, nextCursor);
    }
}
