package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.VolunteerAvailability;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only representation of a volunteer profile.
 */
public record VolunteerDto(
        UUID id,
        String employeeId,
        String fullName,
        String email,
        String phone,
        String baseLocation,
        String department,
        String designation,
        String skills,
        VolunteerAvailability availability,
        Integer totalEventsParticipated,
        BigDecimal avgFeedbackScore
) {
}
