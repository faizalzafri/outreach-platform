package com.outreach.platform.ingestion.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.ingestion.model.DomainEventDocument;
import com.outreach.platform.ingestion.model.EventStatus;
import com.outreach.platform.ingestion.repo.DomainEventRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/** Writes domain events to the MongoDB outbox collection with PENDING status for later publishing. */
@Named
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    public static final String EVENT_VOLUNTEERS_IMPORTED = "VolunteersImported";
    public static final String EVENT_EVENT_SUMMARY_IMPORTED = "EventSummaryImported";
    public static final String EVENT_IMPORT_JOB_COMPLETED = "ImportJobCompleted";

    private final DomainEventRepository domainEventRepository;

    @Inject
    public DomainEventPublisher(DomainEventRepository domainEventRepository) {
        this.domainEventRepository = domainEventRepository;
    }

    /**
     * Publishes a domain event by writing it to the outbox collection.
     */
    public void publish(String eventType, Map<String, Object> payload) {
        DomainEventDocument event = new DomainEventDocument();
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(EventStatus.PENDING);
        event.setCreatedAt(Instant.now());

        if (TenantContext.isPresent()) {
            event.setTenantId(TenantContext.getCurrentTenantId());
        } else {
            log.warn("Persisting domain event without tenantId: TenantContext is empty. eventType={}", eventType);
        }

        domainEventRepository.save(event);
        log.info("Published domain event: type={}, id={}, tenantId={}", eventType, event.getId(), event.getTenantId());
    }

    /**
     * Publishes a VolunteersImported event.
     */
    public void publishVolunteersImported(String jobId, String fileName, int importedCount) {
        publish(EVENT_VOLUNTEERS_IMPORTED, Map.of(
                "jobId", jobId,
                "fileName", fileName,
                "importedCount", importedCount
        ));
    }

    /**
     * Publishes an EventSummaryImported event.
     */
    public void publishEventSummaryImported(String jobId, String fileName, int importedCount) {
        publish(EVENT_EVENT_SUMMARY_IMPORTED, Map.of(
                "jobId", jobId,
                "fileName", fileName,
                "importedCount", importedCount
        ));
    }

    /**
     * Publishes an ImportJobCompleted event.
     */
    public void publishImportJobCompleted(String jobId, String fileName, String status,
                                          int totalRows, int processedRows, int errorCount) {
        publish(EVENT_IMPORT_JOB_COMPLETED, Map.of(
                "jobId", jobId,
                "fileName", fileName,
                "status", status,
                "totalRows", totalRows,
                "processedRows", processedRows,
                "errorCount", errorCount
        ));
    }
}
