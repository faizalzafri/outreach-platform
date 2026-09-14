package com.outreach.platform.notification.service;

import com.outreach.platform.common.tenant.TenantContext;
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
import java.util.Optional;
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
        return toDto(findEntityOrThrow(id));
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
        NotificationTemplateEntity entity = findEntityOrThrow(id);

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
        // deleteById() is even more dangerous than findById() here: Spring Data JPA's default
        // SimpleJpaRepository.deleteById() implementation internally calls findById() and then
        // removes the result — so this was not just a cross-tenant read risk but a cross-tenant
        // delete risk. Route through the same tenant-scoped lookup and an explicit delete(entity)
        // call instead. See docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        templateRepository.delete(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public TemplatePreviewResponse previewTemplate(UUID templateId, TemplatePreviewRequest request) {
        NotificationTemplateEntity entity = findEntityOrThrow(templateId);

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

    private NotificationTemplateEntity findEntityOrThrow(UUID id) {
        // findById() alone does not enforce tenant isolation on this codebase's Hibernate version —
        // see docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        Optional<NotificationTemplateEntity> entity = TenantContext.isPresent()
                ? templateRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                : templateRepository.findById(id);
        return entity.orElseThrow(() -> new TemplateNotFoundException(id));
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
