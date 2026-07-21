package com.outreach.platform.notification.controller;

import com.outreach.platform.notification.model.dto.TemplateCreateRequest;
import com.outreach.platform.notification.model.dto.TemplateDto;
import com.outreach.platform.notification.model.dto.TemplatePreviewRequest;
import com.outreach.platform.notification.model.dto.TemplatePreviewResponse;
import com.outreach.platform.notification.model.dto.TemplateUpdateRequest;
import com.outreach.platform.notification.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for notification template CRUD and preview operations.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/notifications/templates")
@Tag(name = "Notification Templates", description = "Notification template CRUD, preview, and management operations")
public class TemplateController {

    private final TemplateService templateService;

    @Inject
    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public Page<TemplateDto> listTemplates(Pageable pageable) {
        return templateService.listTemplates(pageable);
    }

    @PostMapping
    public ResponseEntity<TemplateDto> createTemplate(@Valid @RequestBody TemplateCreateRequest request) {
        TemplateDto created = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public TemplateDto updateTemplate(@PathVariable UUID id,
                                      @Valid @RequestBody TemplateUpdateRequest request) {
        return templateService.updateTemplate(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/preview")
    public TemplatePreviewResponse previewTemplate(@PathVariable UUID id,
                                                   @Valid @RequestBody TemplatePreviewRequest request) {
        return templateService.previewTemplate(id, request);
    }
}
