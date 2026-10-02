package com.outreach.platform.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.report.entity.ReportScheduleEntity;
import com.outreach.platform.report.repo.ReportScheduleRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends scheduled reports when they are due: the report is rendered in the schedule's format and
 * handed to notification-service, which emails it to the recipients.
 */
@Component
public class ScheduledReportRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final ReportScheduleRepository scheduleRepository;
    private final ReportService reportService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Inject
    public ScheduledReportRunner(JdbcTemplate jdbcTemplate, ReportScheduleRepository scheduleRepository,
                                 ReportService reportService, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                                 PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jdbcTemplate = jdbcTemplate;
        this.scheduleRepository = scheduleRepository;
        this.reportService = reportService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${report-service.schedules.check-interval-ms:60000}")
    public void runDueSchedules() {
        // Due schedules of every tenant; each is then handled under its own tenant.
        List<Map<String, Object>> due = jdbcTemplate.queryForList("SELECT id, tenant_id FROM report_schedules "
                + "WHERE status = 'ACTIVE' AND next_run_at IS NOT NULL AND next_run_at <= now()");
        for (Map<String, Object> row : due) {
            UUID id = (UUID) row.get("id");
            TenantContext.setCurrentTenantId((UUID) row.get("tenant_id"));
            try {
                // One read-write transaction per schedule: its lookup and the update after sending.
                transactionTemplate.executeWithoutResult(tx ->
                        scheduleRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId()).ifPresent(this::run));
            } catch (Exception e) {
                log.error("Scheduled report {} failed; it will be tried again at its next run", id, e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    void run(ReportScheduleEntity schedule) {
        Map<String, Object> filters;
        List<String> recipients;
        byte[] file;
        try {
            filters = schedule.getFilterCriteria() == null ? Map.of()
                    : objectMapper.readValue(schedule.getFilterCriteria(), new TypeReference<>() { });
            recipients = objectMapper.readValue(schedule.getRecipients(), new TypeReference<>() { });
            file = ExportService.render(schedule.getExportFormat(),
                    reportService.aggregateByEvent(ExportService.toQueryParams(filters)));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }

        rabbitTemplate.convertAndSend(RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS,
                RabbitMqConstants.ROUTING_KEY_REPORT_SCHEDULED,
                new DomainEventMessage(UUID.randomUUID().toString(), "ScheduledReportReady", Map.of(
                        "name", schedule.getName(),
                        "fileName", ExportService.buildFileName(schedule.getExportFormat()),
                        "content", Base64.getEncoder().encodeToString(file),
                        "recipients", recipients)));

        schedule.setLastRunAt(Instant.now());
        schedule.setNextRunAt(ScheduledReportService.nextRun(schedule.getCronExpression()));
        scheduleRepository.save(schedule);
        log.info("Sent scheduled report {} to {} recipient(s)", schedule.getId(), recipients.size());
    }
}
