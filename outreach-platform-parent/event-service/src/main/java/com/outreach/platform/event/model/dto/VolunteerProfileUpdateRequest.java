package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.VolunteerAvailability;

/**
 * Request to update a volunteer's profile.
 */
public record VolunteerProfileUpdateRequest(
        String fullName,
        String email,
        String phone,
        String baseLocation,
        String department,
        String designation,
        String skills,
        VolunteerAvailability availability
) {
}
