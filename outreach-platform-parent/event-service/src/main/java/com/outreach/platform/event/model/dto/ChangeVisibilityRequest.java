package com.outreach.platform.event.model.dto;

import com.outreach.platform.event.model.Visibility;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing the visibility of a resource.
 */
public record ChangeVisibilityRequest(

        @NotNull(message = "Visibility is required")
        Visibility visibility
) {
}
