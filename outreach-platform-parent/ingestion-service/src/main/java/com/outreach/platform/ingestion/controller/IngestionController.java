package com.outreach.platform.ingestion.controller;

import com.outreach.platform.ingestion.model.FileUploadResponse;
import com.outreach.platform.ingestion.model.ParseResult;
import com.outreach.platform.ingestion.model.ValidationResult;
import com.outreach.platform.ingestion.service.FileParserService;
import com.outreach.platform.ingestion.service.JobTrackingService;
import jakarta.inject.Inject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
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
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private static final String[] TEMPLATE_HEADERS = {
            "employeeId", "fullName", "email", "phone",
            "baseLocation", "department", "designation", "skills", "eventCode"
    };

    private final FileParserService fileParserService;
    private final JobTrackingService jobTrackingService;

    @Inject
    public IngestionController(FileParserService fileParserService,
                               JobTrackingService jobTrackingService) {
        this.fileParserService = fileParserService;
        this.jobTrackingService = jobTrackingService;
    }

    /**
     * Upload a single file for processing. Returns 202 Accepted with a job ID.
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {

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

        FileUploadResponse response = new FileUploadResponse(
                jobId,
                file.getOriginalFilename(),
                "ACCEPTED",
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Upload multiple files for processing. Returns 202 Accepted with a list of job IDs.
     */
    @PostMapping("/upload/bulk")
    public ResponseEntity<List<FileUploadResponse>> uploadBulk(
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            validateNotEmpty(file);
            String extension = fileParserService.getExtension(file.getOriginalFilename());
            fileParserService.validateExtension(extension);

            UUID jobId = UUID.randomUUID();
            log.info("Accepted bulk file upload: {} (jobId={})", file.getOriginalFilename(), jobId);

            jobTrackingService.createJob(
                    jobId.toString(),
                    "FILE_IMPORT",
                    file.getOriginalFilename(),
                    extension,
                    file.getSize()
            );

            responses.add(new FileUploadResponse(
                    jobId,
                    file.getOriginalFilename(),
                    "ACCEPTED",
                    Instant.now()
            ));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responses);
    }

    /**
     * Dry-run validation: parse and validate the file without importing data.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateFile(
            @RequestParam("file") MultipartFile file) throws IOException {

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
     * Download the volunteer import template (empty Excel with correct headers).
     */
    @GetMapping("/templates")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] content = generateTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "volunteer-import-template.xlsx");
        headers.setContentLength(content.length);

        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    private byte[] generateTemplate() throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1)) {
            Sheet sheet = workbook.createSheet("Volunteers");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(TEMPLATE_HEADERS[i]);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
    }
}
