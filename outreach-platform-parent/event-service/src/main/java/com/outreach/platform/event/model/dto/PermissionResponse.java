package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.PermissionLevel;
import com.outreach.platform.event.model.ResourceType;
import com.outreach.platform.event.model.Visibility;

import java.util.UUID;

/**
 * Response DTO for a resource permission record.
 */
public record PermissionResponse(
        UUID id,
        ResourceType resourceType,
        UUID resourceId,
        Visibility visibility,
        UUID ownerUserId,
        UUID grantedTeamId,
        PermissionLevel permissionLevel
) {
}
