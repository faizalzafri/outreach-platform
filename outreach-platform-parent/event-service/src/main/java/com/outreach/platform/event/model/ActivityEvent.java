package com.outreach.platform.event.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB document representing an activity event in the activity feed.
 * Stores tenant-scoped activity for resource creation, modification, sharing,
 * team membership changes, and sequence activations.
 * <p>
 * A TTL index on {@code timestamp} enforces 90-day retention (7,776,000 seconds).
 */
@Document(collection = "activity_events")
@CompoundIndexes({
        @CompoundIndex(name = "idx_tenant_timestamp", def = "{'tenantId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_tenant_team_timestamp", def = "{'tenantId': 1, 'teamId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_tenant_actor", def = "{'tenantId': 1, 'actorUserId': 1}")
})
public class ActivityEvent {

    @Id
    private String id;

    private UUID activityId;

    private UUID tenantId;

    private UUID actorUserId;

    private ActionType actionType;

    private String resourceType;

    private UUID resourceId;

    private UUID teamId;

    @Indexed(name = "idx_ttl_retention", expireAfter = "90d")
    private Instant timestamp;

    private Map<String, Object> metadata;

    public ActivityEvent() {
    }

    public ActivityEvent(UUID tenantId, UUID actorUserId, ActionType actionType,
                         String resourceType, UUID resourceId, UUID teamId,
                         Map<String, Object> metadata) {
        this.id = UUID.randomUUID().toString();
        this.activityId = UUID.randomUUID();
        this.tenantId = tenantId;
        this.actorUserId = actorUserId;
        this.actionType = actionType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.teamId = teamId;
        this.timestamp = Instant.now();
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public void setActivityId(UUID activityId) {
        this.activityId = activityId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
