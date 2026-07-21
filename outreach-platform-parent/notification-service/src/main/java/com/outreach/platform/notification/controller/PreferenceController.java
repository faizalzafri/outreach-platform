package com.outreach.platform.notification.controller;

import com.outreach.platform.notification.model.dto.PreferenceDto;
import com.outreach.platform.notification.model.dto.PreferenceUpdateRequest;
import com.outreach.platform.notification.service.NotificationPreferenceService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing notification preferences per employee.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/notifications/preferences")
public class PreferenceController {

    private final NotificationPreferenceService preferenceService;

    @Inject
    public PreferenceController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    /**
     * GET /notifications/preferences/{employeeId} — get notification preferences.
     */
    @GetMapping("/{employeeId}")
    public PreferenceDto getPreferences(@PathVariable String employeeId) {
        return preferenceService.getPreferences(employeeId);
    }

    /**
     * PUT /notifications/preferences/{employeeId} — update notification preferences.
     */
    @PutMapping("/{employeeId}")
    public PreferenceDto updatePreferences(@PathVariable String employeeId,
                                           @Valid @RequestBody PreferenceUpdateRequest request) {
        return preferenceService.updatePreferences(employeeId, request);
    }
}
