package com.outreach.platform.ingestion.controller;

import com.outreach.platform.ingestion.model.FileUploadResponse;
import com.outreach.platform.ingestion.model.ParseResult;
import com.outreach.platform.ingestion.model.ValidationResult;
import com.outreach.platform.ingestion.service.FileParserService;
import com.outreach.platform.ingestion.service.ImportProcessingService;
import com.outreach.platform.ingestion.service.ImportTemplateService;
import com.outreach.platform.ingestion.service.JobTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for file ingestion operations: upload, bulk upload,
 * dry-run validation, and template download.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/ingestion")
@Tag(name = "File Ingestion", description = "File upload, validation, and template download for data import")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final FileParserService fileParserService;
    private final JobTrackingService jobTrackingService;
    private final ImportProcessingService importProcessingService;
    private final ImportTemplateService templates;

    @Inject
    public IngestionController(FileParserService fileParserService,
                               JobTrackingService jobTrackingService,
                               ImportProcessingService importProcessingService,
                               ImportTemplateService templates) {
        this.templates = templates;
        this.fileParserService = fileParserService;
        this.jobTrackingService = jobTrackingService;
        this.importProcessingService = importProcessingService;
    }

    /**
     * Upload a single file for processing. Returns 202 Accepted with a job ID.
     */
    @Operation(summary = "Upload file", description = "Uploads a single file for processing, returns 202 Accepted with a job ID")
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(acceptForImport(file));
    }

    /**
     * Upload multiple files for processing. Returns 202 Accepted with a list of job IDs.
     */
    @Operation(summary = "Bulk upload", description = "Uploads multiple files for processing, returns 202 Accepted with job IDs")
    @PostMapping("/upload/bulk")
    public ResponseEntity<List<FileUploadResponse>> uploadBulk(
            @Parameter(description = "Files to upload") @RequestParam("files") List<MultipartFile> files) throws IOException {

        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(acceptForImport(file));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responses);
    }

    /** Validates the file, records a PENDING job, and hands it to the async import pipeline. */
    private FileUploadResponse acceptForImport(MultipartFile file) throws IOException {
        validateNotEmpty(file);
        String extension = fileParserService.getExtension(file.getOriginalFilename());
        fileParserService.validateExtension(extension);

        UUID jobId = UUID.randomUUID();
        log.info("Accepted file upload: {} (jobId={})", file.getOriginalFilename(), jobId);

        jobTrackingService.createJob(
                jobId.toString(),
                "FILE_IMPORT",
                file.getOriginalFilename(),
                extension,
                file.getSize()
        );

        // Captured synchronously — the MultipartFile's backing temp file isn't guaranteed to
        // survive past this request, but the @Async processing below runs on a separate thread.
        byte[] fileBytes = file.getBytes();
        importProcessingService.processImport(jobId.toString(), fileBytes, file.getOriginalFilename(), extension);

        return new FileUploadResponse(jobId, file.getOriginalFilename(), "ACCEPTED", Instant.now());
    }

    /**
     * Dry-run validation: parse and validate the file without importing data.
     */
    @Operation(summary = "Validate file", description = "Dry-run validation: parses and validates a file without importing data")
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateFile(
            @Parameter(description = "File to validate") @RequestParam("file") MultipartFile file) throws IOException {

        validateNotEmpty(file);

        ParseResult parseResult = fileParserService.parseAndValidate(file);

        ValidationResult result = new ValidationResult(
                file.getOriginalFilename(),
                parseResult.totalRows(),
                parseResult.validCount(),
                parseResult.errorCount(),
                parseResult.errors()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Download the volunteer import template in any supported format, with one example row.
     */
    @Operation(summary = "Download template",
            description = "Volunteer import template as xlsx (default), xls or csv: headers plus one example row")
    @GetMapping("/templates")
    public ResponseEntity<byte[]> downloadTemplate(
            @Parameter(description = "xlsx, xls or csv") @RequestParam(defaultValue = "xlsx") String format) {
        ImportTemplateService.Format template = ImportTemplateService.Format.of(format);
        byte[] content = templates.render(template);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(template.contentType()));
        headers.setContentDispositionFormData("attachment", template.fileName());
        headers.setContentLength(content.length);

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
    }
}
