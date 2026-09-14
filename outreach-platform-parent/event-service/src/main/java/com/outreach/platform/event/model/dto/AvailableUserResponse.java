package com.outreach.platform.event.model.dto;

import java.util.UUID;

/**
 * Lightweight user summary for the "add team member" searchable selector — deliberately excludes
 * role/status/audit fields a team-management UI doesn't need.
 */
public record AvailableUserResponse(
        UUID id,
        String username,
        String email
) {
}
