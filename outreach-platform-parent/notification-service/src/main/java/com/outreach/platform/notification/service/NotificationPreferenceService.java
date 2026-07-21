package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.NotificationPreferenceDocument;
import com.outreach.platform.notification.model.dto.PreferenceDto;
import com.outreach.platform.notification.model.dto.PreferenceUpdateRequest;
import com.outreach.platform.notification.repo.NotificationPreferenceRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Manages notification preferences per employee — retrieval and updates.
 */
@Service
public class NotificationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;

    @Inject
    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Retrieves notification preferences for an employee.
     * Returns default preferences if none exist yet.
     */
    public PreferenceDto getPreferences(String employeeId) {
        NotificationPreferenceDocument doc = preferenceRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> createDefault(employeeId));
        return toDto(doc);
    }

    /**
     * Updates notification preferences for an employee.
     * Creates the preference document if it doesn't exist.
     */
    public PreferenceDto updatePreferences(String employeeId, PreferenceUpdateRequest request) {
        NotificationPreferenceDocument doc = preferenceRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> {
                    NotificationPreferenceDocument newDoc = new NotificationPreferenceDocument();
                    newDoc.setEmployeeId(employeeId);
                    newDoc.setCreatedAt(Instant.now());
                    return newDoc;
                });

        doc.setEmailEnabled(request.emailEnabled());
        doc.setAllowedTypes(request.allowedTypes() != null ? request.allowedTypes() : List.of());
        doc.setUpdatedAt(Instant.now());

        NotificationPreferenceDocument saved = preferenceRepository.save(doc);
        log.info("Updated notification preferences for employee: {}", employeeId);
        return toDto(saved);
    }

    private NotificationPreferenceDocument createDefault(String employeeId) {
        NotificationPreferenceDocument doc = new NotificationPreferenceDocument();
        doc.setEmployeeId(employeeId);
        doc.setEmailEnabled(true);
        doc.setAllowedTypes(List.of());
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return preferenceRepository.save(doc);
    }

    private PreferenceDto toDto(NotificationPreferenceDocument doc) {
        return new PreferenceDto(
                doc.getEmployeeId(),
                doc.isEmailEnabled(),
                doc.getAllowedTypes(),
                doc.getUpdatedAt()
        );
    }
}
