package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.config.IngestionServiceProperties;
import com.outreach.platform.ingestion.model.ParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileParserServiceTest {

    private FileParserService service;

    @BeforeEach
    void setUp() {
        IngestionServiceProperties props = new IngestionServiceProperties(
                "/tmp/input", 25, List.of(".xlsx", ".xls", ".csv"), 5
        );
        service = new FileParserService(new ExcelParser(), new CsvParser(), new RowValidator(), props);
    }

    @Test
    void parseAndValidate_validCsv_returnsAllValid() throws IOException {
        String csv = """
                employeeId,fullName,email,phone,eventCode
                EMP001,John Doe,john@example.com,123-456,EVT01
                EMP002,Jane Smith,jane@example.com,789-012,EVT02
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParseResult result = service.parseAndValidate(file);

        assertEquals(2, result.totalRows());
        assertEquals(2, result.validCount());
        assertEquals(0, result.errorCount());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void parseAndValidate_invalidRows_returnsErrors() throws IOException {
        String csv = """
                employeeId,fullName,email,phone,eventCode
                ,,,bad-phone!,
                EMP002,Jane Smith,jane@example.com,,EVT02
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParseResult result = service.parseAndValidate(file);

        assertEquals(2, result.totalRows());
        assertEquals(1, result.validCount());
        assertEquals(1, result.errorCount());
        assertFalse(result.errors().isEmpty());
    }

    @Test
    void getExtension_variousFilenames() {
        assertEquals(".csv", service.getExtension("data.csv"));
        assertEquals(".xlsx", service.getExtension("report.xlsx"));
        assertEquals(".xls", service.getExtension("old.xls"));
        assertEquals("", service.getExtension("noextension"));
        assertEquals("", service.getExtension(null));
    }

    @Test
    void validateExtension_unsupported_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validateExtension(".pdf"));
    }

    @Test
    void validateExtension_supported_doesNotThrow() {
        assertDoesNotThrow(() -> service.validateExtension(".csv"));
        assertDoesNotThrow(() -> service.validateExtension(".xlsx"));
        assertDoesNotThrow(() -> service.validateExtension(".xls"));
    }
}
