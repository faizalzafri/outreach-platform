package com.outreach.platform.notification.service;

import com.outreach.platform.notification.entity.NotificationTemplateEntity;
import com.outreach.platform.notification.model.dto.TemplateCreateRequest;
import com.outreach.platform.notification.model.dto.TemplateDto;
import com.outreach.platform.notification.model.dto.TemplatePreviewRequest;
import com.outreach.platform.notification.model.dto.TemplatePreviewResponse;
import com.outreach.platform.notification.model.dto.TemplateUpdateRequest;
import com.outreach.platform.notification.repo.NotificationTemplateRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD operations for notification templates.
 */
@Service
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateRenderingService renderingService;
    private final TemplateValidationService validationService;

    @Inject
    public TemplateService(NotificationTemplateRepository templateRepository,
                           TemplateRenderingService renderingService,
                           TemplateValidationService validationService) {
        this.templateRepository = templateRepository;
        this.renderingService = renderingService;
        this.validationService = validationService;
    }

    @Transactional(readOnly = true)
    public Page<TemplateDto> listTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public TemplateDto getTemplate(UUID id) {
        NotificationTemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException(id));
        return toDto(entity);
    }

    @Transactional
    public TemplateDto createTemplate(TemplateCreateRequest request) {
        NotificationTemplateEntity entity = new NotificationTemplateEntity();
        entity.setName(request.name());
        entity.setType(request.type());
        entity.setSubjectTemplate(request.subjectTemplate());
        entity.setBodyTemplate(request.bodyTemplate());
        entity.setEngine(request.engine());
        entity.setVariablesSchema(request.variablesSchema());
        entity.setActive(true);

        NotificationTemplateEntity saved = templateRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public TemplateDto updateTemplate(UUID id, TemplateUpdateRequest request) {
        NotificationTemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException(id));

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.type() != null) {
            entity.setType(request.type());
        }
        if (request.subjectTemplate() != null) {
            entity.setSubjectTemplate(request.subjectTemplate());
        }
        if (request.bodyTemplate() != null) {
            entity.setBodyTemplate(request.bodyTemplate());
        }
        if (request.engine() != null) {
            entity.setEngine(request.engine());
        }
        if (request.variablesSchema() != null) {
            entity.setVariablesSchema(request.variablesSchema());
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }

        NotificationTemplateEntity saved = templateRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        if (!templateRepository.existsById(id)) {
            throw new TemplateNotFoundException(id);
        }
        templateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TemplatePreviewResponse previewTemplate(UUID templateId, TemplatePreviewRequest request) {
        NotificationTemplateEntity entity = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));

        // Validate variables against schema
        List<String> validationErrors = validationService.validate(
                entity.getVariablesSchema(), request.variables());
        if (!validationErrors.isEmpty()) {
            throw new TemplateVariableValidationException(validationErrors);
        }

        String renderedSubject = renderingService.render(
                entity.getSubjectTemplate() != null ? entity.getSubjectTemplate() : "",
                request.variables());
        String renderedBody = renderingService.render(
                entity.getBodyTemplate(), request.variables());

        return new TemplatePreviewResponse(renderedSubject, renderedBody);
    }

    private TemplateDto toDto(NotificationTemplateEntity entity) {
        return new TemplateDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getSubjectTemplate(),
                entity.getBodyTemplate(),
                entity.getEngine(),
                entity.getVariablesSchema(),
                entity.isActive(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy()
        );
    }

    /**
     * Thrown when a template is not found by ID.
     */
    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(UUID id) {
            super("Notification template not found: " + id);
        }
    }

    /**
     * Thrown when template variables fail schema validation.
     */
    public static class TemplateVariableValidationException extends RuntimeException {
        private final List<String> errors;

        public TemplateVariableValidationException(List<String> errors) {
            super("Template variable validation failed: " + String.join("; ", errors));
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
