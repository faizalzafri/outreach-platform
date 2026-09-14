package com.outreach.platform.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.report.entity.ReportScheduleEntity;
import com.outreach.platform.report.model.ScheduleStatus;
import com.outreach.platform.report.model.ScheduledReportCreateRequest;
import com.outreach.platform.report.model.ScheduledReportDto;
import com.outreach.platform.report.model.ScheduledReportUpdateRequest;
import com.outreach.platform.report.repo.ReportScheduleRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Manages CRUD operations for scheduled report configurations stored in PostgreSQL. */
@Service
public class ScheduledReportService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportService.class);

    private final ReportScheduleRepository scheduleRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public ScheduledReportService(ReportScheduleRepository scheduleRepository, ObjectMapper objectMapper) {
        this.scheduleRepository = scheduleRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ScheduledReportDto> listScheduledReports() {
        return scheduleRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ScheduledReportDto> getScheduledReport(UUID id) {
        return findEntityById(id).map(this::toDto);
    }

    @Transactional
    public ScheduledReportDto createScheduledReport(ScheduledReportCreateRequest request) {
        ReportScheduleEntity entity = new ReportScheduleEntity();
        entity.setName(request.name());
        entity.setReportType(request.reportType());
        entity.setCronExpression(request.cronExpression());
        entity.setExportFormat(request.exportFormat());
        entity.setFilterCriteria(serializeJson(request.filterCriteria()));
        entity.setRecipients(serializeJson(request.recipients()));
        entity.setStatus(ScheduleStatus.ACTIVE);

        ReportScheduleEntity saved = scheduleRepository.save(entity);
        log.info("Created scheduled report: id={}, name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    public Optional<ScheduledReportDto> updateScheduledReport(UUID id, ScheduledReportUpdateRequest request) {
        return findEntityById(id).map(entity -> {
            if (request.name() != null) {
                entity.setName(request.name());
            }
            if (request.reportType() != null) {
                entity.setReportType(request.reportType());
            }
            if (request.cronExpression() != null) {
                entity.setCronExpression(request.cronExpression());
            }
            if (request.exportFormat() != null) {
                entity.setExportFormat(request.exportFormat());
            }
            if (request.filterCriteria() != null) {
                entity.setFilterCriteria(serializeJson(request.filterCriteria()));
            }
            if (request.recipients() != null) {
                entity.setRecipients(serializeJson(request.recipients()));
            }
            if (request.status() != null) {
                entity.setStatus(request.status());
            }

            ReportScheduleEntity saved = scheduleRepository.save(entity);
            log.info("Updated scheduled report: id={}", saved.getId());
            return toDto(saved);
        });
    }

    @Transactional
    public boolean deleteScheduledReport(UUID id) {
        // deleteById() is more dangerous than findById() here: Spring Data JPA's default
        // SimpleJpaRepository.deleteById() implementation internally calls findById() and then
        // removes the result — so this was a cross-tenant delete risk, not just a read risk.
        // Route through the same tenant-scoped lookup and an explicit delete(entity) call instead.
        // See docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        Optional<ReportScheduleEntity> entity = findEntityById(id);
        entity.ifPresent(e -> {
            scheduleRepository.delete(e);
            log.info("Deleted scheduled report: id={}", id);
        });
        return entity.isPresent();
    }

    private Optional<ReportScheduleEntity> findEntityById(UUID id) {
        // findById() alone does not enforce tenant isolation on this codebase's Hibernate version —
        // see docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        return TenantContext.isPresent()
                ? scheduleRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                : scheduleRepository.findById(id);
    }

    private ScheduledReportDto toDto(ReportScheduleEntity entity) {
        return new ScheduledReportDto(
                entity.getId(),
                entity.getName(),
                entity.getReportType(),
                entity.getCronExpression(),
                entity.getExportFormat(),
                deserializeMap(entity.getFilterCriteria()),
                deserializeList(entity.getRecipients()),
                entity.getStatus(),
                entity.getNextRunAt(),
                entity.getCreatedDate()
        );
    }

    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize JSON value", e);
            return null;
        }
    }

    private Map<String, Object> deserializeMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON map", e);
            return Map.of();
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON list", e);
            return List.of();
        }
    }
}
