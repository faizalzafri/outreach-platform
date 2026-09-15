package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.client.EventServiceClient;
import com.outreach.platform.ingestion.model.ParseResult;
import com.outreach.platform.ingestion.model.ParsedRow;
import com.outreach.platform.ingestion.model.ValidationError;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the actual work of a file-import job: parse, validate, upsert-and-enroll each
 * valid row via event-service, track progress, and publish the resulting domain events.
 *
 * <p>Runs {@code @Async} on the default {@code ThreadPoolTaskExecutor}, which common-lib's
 * {@code TenantAutoConfiguration} decorates with {@code TenantContextTaskDecorator} — so
 * {@code TenantContext} (captured from the original upload request) is available here, and in
 * turn on the outbound Feign call via {@code FeignAuthAutoConfiguration}'s interceptor.
 */
@Named
public class ImportProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImportProcessingService.class);

    private final FileParserService fileParserService;
    private final JobTrackingService jobTrackingService;
    private final EventServiceClient eventServiceClient;
    private final DomainEventPublisher domainEventPublisher;

    @Inject
    public ImportProcessingService(FileParserService fileParserService,
                                   JobTrackingService jobTrackingService,
                                   EventServiceClient eventServiceClient,
                                   DomainEventPublisher domainEventPublisher) {
        this.fileParserService = fileParserService;
        this.jobTrackingService = jobTrackingService;
        this.eventServiceClient = eventServiceClient;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * Processes an uploaded file end to end. Failures on individual rows are recorded and do not
     * abort the job; only a whole-file-level failure (e.g. unreadable file) marks the job FAILED.
     */
    @Async
    public void processImport(String jobId, byte[] fileBytes, String fileName, String extension) {
        try {
            ParseResult parseResult = fileParserService.parseAndValidate(new ByteArrayInputStream(fileBytes), extension);
            List<ValidationError> errors = new ArrayList<>(parseResult.errors());

            jobTrackingService.startJob(jobId, parseResult.totalRows());

            Map<UUID, List<Map<String, String>>> volunteersByEvent = new LinkedHashMap<>();
            int processed = 0;
            int rowsHandled = 0;

            for (ParsedRow row : parseResult.validRows()) {
                try {
                    EventServiceClient.VolunteerImportResponse response = eventServiceClient.importVolunteer(
                            new EventServiceClient.VolunteerImportRequest(
                                    field(row, "employeeid"),
                                    field(row, "fullname"),
                                    field(row, "email"),
                                    field(row, "phone"),
                                    field(row, "baselocation"),
                                    field(row, "department"),
                                    field(row, "designation"),
                                    field(row, "skills"),
                                    field(row, "eventcode")
                            ));

                    volunteersByEvent
                            .computeIfAbsent(response.eventId(), k -> new ArrayList<>())
                            .add(Map.of("email", field(row, "email"), "name", field(row, "fullname")));
                    processed++;
                } catch (Exception e) {
                    log.warn("Row {} failed during import: {}", row.rowNumber(), e.getMessage());
                    errors.add(new ValidationError(row.rowNumber(), "eventCode", e.getMessage(), field(row, "eventcode")));
                }
                rowsHandled++;
                jobTrackingService.updateProgress(jobId, rowsHandled, parseResult.totalRows());
            }

            jobTrackingService.completeJob(jobId, processed, errors.size(), errors);

            for (Map.Entry<UUID, List<Map<String, String>>> entry : volunteersByEvent.entrySet()) {
                domainEventPublisher.publishVolunteersImported(entry.getKey(), entry.getValue());
            }
            domainEventPublisher.publishImportJobCompleted(
                    jobId, fileName, "COMPLETED", parseResult.totalRows(), processed, errors.size());
        } catch (Exception e) {
            log.error("Import job {} failed: {}", jobId, e.getMessage(), e);
            jobTrackingService.failJob(jobId, "Import failed: " + e.getMessage());
            domainEventPublisher.publishImportJobCompleted(jobId, fileName, "FAILED", 0, 0, 0);
        }
    }

    private String field(ParsedRow row, String key) {
        return row.fields().getOrDefault(key, "");
    }
}
