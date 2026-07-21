package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.NotificationPreferenceDocument;
import com.outreach.platform.notification.model.dto.PreferenceDto;
import com.outreach.platform.notification.model.dto.PreferenceUpdateRequest;
import com.outreach.platform.notification.repo.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for NotificationPreferenceService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    private NotificationPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new NotificationPreferenceService(preferenceRepository);
    }

    @Test
    void getPreferencesReturnsExistingDocument() {
        String employeeId = "EMP001";
        NotificationPreferenceDocument doc = buildDoc(employeeId, true, List.of("FEEDBACK", "EVENT"));
        when(preferenceRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(doc));

        PreferenceDto result = preferenceService.getPreferences(employeeId);

        assertEquals(employeeId, result.employeeId());
        assertTrue(result.emailEnabled());
        assertEquals(List.of("FEEDBACK", "EVENT"), result.allowedTypes());
    }

    @Test
    void getPreferencesCreatesDefaultWhenNoneExist() {
        String employeeId = "EMP002";
        when(preferenceRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        NotificationPreferenceDocument defaultDoc = buildDoc(employeeId, true, List.of());
        when(preferenceRepository.save(any(NotificationPreferenceDocument.class))).thenReturn(defaultDoc);

        PreferenceDto result = preferenceService.getPreferences(employeeId);

        assertEquals(employeeId, result.employeeId());
        assertTrue(result.emailEnabled());
        assertNotNull(result.updatedAt());
        verify(preferenceRepository).save(any(NotificationPreferenceDocument.class));
    }

    @Test
    void updatePreferencesModifiesExistingDocument() {
        String employeeId = "EMP003";
        NotificationPreferenceDocument doc = buildDoc(employeeId, true, List.of());
        when(preferenceRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(doc));

        PreferenceUpdateRequest request = new PreferenceUpdateRequest(false, List.of("WELCOME"));

        NotificationPreferenceDocument updated = buildDoc(employeeId, false, List.of("WELCOME"));
        when(preferenceRepository.save(any(NotificationPreferenceDocument.class))).thenReturn(updated);

        PreferenceDto result = preferenceService.updatePreferences(employeeId, request);

        assertEquals(false, result.emailEnabled());
        assertEquals(List.of("WELCOME"), result.allowedTypes());

        ArgumentCaptor<NotificationPreferenceDocument> captor = ArgumentCaptor.forClass(NotificationPreferenceDocument.class);
        verify(preferenceRepository).save(captor.capture());
        assertEquals(false, captor.getValue().isEmailEnabled());
    }

    @Test
    void updatePreferencesCreatesNewDocumentWhenNoneExist() {
        String employeeId = "EMP004";
        when(preferenceRepository.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        PreferenceUpdateRequest request = new PreferenceUpdateRequest(true, List.of("EVENT"));

        NotificationPreferenceDocument saved = buildDoc(employeeId, true, List.of("EVENT"));
        when(preferenceRepository.save(any(NotificationPreferenceDocument.class))).thenReturn(saved);

        PreferenceDto result = preferenceService.updatePreferences(employeeId, request);

        assertEquals(employeeId, result.employeeId());
        assertTrue(result.emailEnabled());
        assertEquals(List.of("EVENT"), result.allowedTypes());
    }

    private NotificationPreferenceDocument buildDoc(String employeeId, boolean emailEnabled, List<String> types) {
        NotificationPreferenceDocument doc = new NotificationPreferenceDocument();
        doc.setId("mongo-id-" + employeeId);
        doc.setEmployeeId(employeeId);
        doc.setEmailEnabled(emailEnabled);
        doc.setAllowedTypes(types);
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return doc;
    }
}
