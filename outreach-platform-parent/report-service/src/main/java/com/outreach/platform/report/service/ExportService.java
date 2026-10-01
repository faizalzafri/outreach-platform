package com.outreach.platform.report.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.report.model.EventScoreDto;
import com.outreach.platform.report.model.ExportFormat;
import com.outreach.platform.report.model.ExportJobDocument;
import com.outreach.platform.report.model.ExportJobDto;
import com.outreach.platform.report.model.ExportJobStatus;
import com.outreach.platform.report.model.ExportRequest;
import com.outreach.platform.report.model.ReportQueryParams;
import com.outreach.platform.report.repo.ExportJobRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Asynchronous report export: renders the tenant-scoped by-event report (the same data as the
 * Reports page's "By Event" tab, with the same filters) as CSV, Excel or PDF, tracking each job
 * in MongoDB. Jobs belong to the tenant that submitted them.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private static final List<String> HEADERS = List.of(
            "Event ID", "Event Name", "City", "Average Score", "Feedback Count", "Min Score", "Max Score");

    private final ExportJobRepository exportJobRepository;
    private final ReportService reportService;
    private final Executor exportTaskExecutor;

    @Inject
    public ExportService(ExportJobRepository exportJobRepository,
                         ReportService reportService,
                         @Named("exportTaskExecutor") Executor exportTaskExecutor) {
        this.exportJobRepository = exportJobRepository;
        this.reportService = reportService;
        this.exportTaskExecutor = exportTaskExecutor;
    }

    /** Submits a new export job and triggers async generation. */
    public String submitExport(ExportRequest request) {
        String jobId = UUID.randomUUID().toString();

        ExportJobDocument job = new ExportJobDocument();
        job.setJobId(jobId);
        job.setTenantId(TenantContext.getCurrentTenantId());
        job.setJobType("REPORT_EXPORT");
        job.setFormat(request.format());
        job.setStatus(ExportJobStatus.PENDING);
        job.setFilterCriteria(request.filters());
        job.setCreatedAt(Instant.now());

        exportJobRepository.save(job);

        // Submitted to the executor directly: an @Async method called via `this` bypasses the proxy
        // and would run synchronously on the request thread. The executor carries TenantContext
        // over (see AsyncConfig), so the report query runs for the submitting tenant.
        exportTaskExecutor.execute(() -> generateExport(jobId, request.format(), request.filters()));

        return jobId;
    }

    /** Retrieves the current status of an export job. */
    public ExportJobDto getExportStatus(String jobId) {
        return findJob(jobId).map(this::toDto).orElse(null);
    }

    /** Retrieves the file content bytes for a completed export job, or null if unavailable. */
    public byte[] getExportContent(String jobId) {
        return findJob(jobId)
                .filter(job -> job.getStatus() == ExportJobStatus.COMPLETED)
                .map(ExportJobDocument::getFileContent)
                .orElse(null);
    }

    private Optional<ExportJobDocument> findJob(String jobId) {
        // The job holds a tenant's report data; an empty TenantContext (PLATFORM_ADMIN) may read any.
        return TenantContext.isPresent()
                ? exportJobRepository.findByJobIdAndTenantId(jobId, TenantContext.getCurrentTenantId())
                : exportJobRepository.findByJobId(jobId);
    }

    private void generateExport(String jobId, ExportFormat format, Map<String, Object> filters) {
        log.info("Starting async export generation for job: {}, format: {}", jobId, format);

        ExportJobDocument job = findJob(jobId).orElse(null);
        if (job == null) {
            log.error("Export job not found: {}", jobId);
            return;
        }

        job.setStatus(ExportJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        exportJobRepository.save(job);

        try {
            List<EventScoreDto> rows = reportService.aggregateByEvent(toQueryParams(filters));
            job.setFileContent(render(format, rows));
            job.setFileName(buildFileName(format));
            job.setStatus(ExportJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);

            log.info("Export job completed: {}, rows: {}", jobId, rows.size());
        } catch (Exception e) {
            log.error("Export job failed: {}", jobId, e);
            job.setStatus(ExportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);
        }
    }

    /**
     * Maps the Reports page's export filters onto the report query. Unknown keys (e.g.
     * granularity-only UI state) are ignored; malformed dates fail the job.
     */
    static ReportQueryParams toQueryParams(Map<String, Object> filters) {
        Map<String, Object> f = filters != null ? filters : Map.of();
        return new ReportQueryParams(
                date(f.get("startDate")), date(f.get("endDate")),
                list(f.get("eventIds")), list(f.get("cities")),
                list(f.get("beneficiaries")), list(f.get("pocIds")),
                f.get("granularity") != null ? f.get("granularity").toString() : null);
    }

    private static LocalDate date(Object value) {
        return value == null || value.toString().isBlank() ? null : LocalDate.parse(value.toString());
    }

    private static List<String> list(Object value) {
        return value instanceof List<?> items ? items.stream().map(String::valueOf).toList() : null;
    }

    private static byte[] render(ExportFormat format, List<EventScoreDto> rows) throws IOException {
        List<List<String>> table = rows.stream().map(ExportService::cells).toList();
        return switch (format) {
            case CSV -> renderCsv(table);
            case EXCEL -> renderExcel(rows);
            case PDF -> renderPdf(table);
        };
    }

    private static List<String> cells(EventScoreDto e) {
        return List.of(e.eventId(), nullToEmpty(e.eventName()), nullToEmpty(e.city()),
                e.averageScore().toPlainString(), String.valueOf(e.feedbackCount()),
                String.valueOf(e.minScore()), String.valueOf(e.maxScore()));
    }

    private static byte[] renderCsv(List<List<String>> table) {
        StringBuilder csv = new StringBuilder(csvLine(HEADERS));
        table.forEach(row -> csv.append(csvLine(row)));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvLine(List<String> values) {
        return values.stream().map(ExportService::csvCell).collect(Collectors.joining(",")) + "\r\n";
    }

    /** RFC 4180 quoting, plus a leading quote on text spreadsheets would run as a formula. */
    private static String csvCell(String value) {
        String safe = !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static byte[] renderExcel(List<EventScoreDto> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Event Scores");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
            }
            int r = 1;
            for (EventScoreDto e : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(e.eventId());
                row.createCell(1).setCellValue(nullToEmpty(e.eventName()));
                row.createCell(2).setCellValue(nullToEmpty(e.city()));
                row.createCell(3).setCellValue(e.averageScore().doubleValue());
                row.createCell(4).setCellValue(e.feedbackCount());
                row.createCell(5).setCellValue(e.minScore());
                row.createCell(6).setCellValue(e.maxScore());
            }
            for (int i = 0; i < HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static byte[] renderPdf(List<List<String>> table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Event Scores Report",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        document.add(new Paragraph("Generated " + Instant.now(), FontFactory.getFont(FontFactory.HELVETICA, 9)));

        PdfPTable pdfTable = new PdfPTable(HEADERS.size());
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(10);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        HEADERS.forEach(h -> pdfTable.addCell(new Phrase(h, headerFont)));
        table.forEach(row -> row.forEach(cell -> pdfTable.addCell(new Phrase(cell, cellFont))));
        document.add(pdfTable);
        document.close();
        return out.toByteArray();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String buildFileName(ExportFormat format) {
        String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        return switch (format) {
            case CSV -> "report_" + timestamp + ".csv";
            case EXCEL -> "report_" + timestamp + ".xlsx";
            case PDF -> "report_" + timestamp + ".pdf";
        };
    }

    private ExportJobDto toDto(ExportJobDocument doc) {
        return new ExportJobDto(
                doc.getJobId(),
                doc.getStatus(),
                doc.getFormat(),
                doc.getFileName(),
                doc.getErrorMessage(),
                doc.getCreatedAt(),
                doc.getCompletedAt()
        );
    }
}
