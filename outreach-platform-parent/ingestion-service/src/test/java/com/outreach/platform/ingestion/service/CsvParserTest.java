package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.ParsedRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private CsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvParser();
    }

    @Test
    void parsesSimpleCsv() throws IOException {
        String csv = """
                employeeId,fullName,email,eventCode
                EMP001,John Doe,john@example.com,EVT-01
                EMP002,Jane Smith,jane@example.com,EVT-02
                """;

        List<ParsedRow> rows = parser.parse(toStream(csv));

        assertEquals(2, rows.size());
        assertEquals("EMP001", rows.get(0).fields().get("employeeid"));
        assertEquals("jane@example.com", rows.get(1).fields().get("email"));
    }

    @Test
    void handlesQuotedFields() throws IOException {
        String csv = """
                name,description,code
                "Smith, John","Has a comma, inside",CODE1
                """;

        List<ParsedRow> rows = parser.parse(toStream(csv));

        assertEquals(1, rows.size());
        assertEquals("Smith, John", rows.get(0).fields().get("name"));
        assertEquals("Has a comma, inside", rows.get(0).fields().get("description"));
    }

    @Test
    void skipsBlankLines() throws IOException {
        String csv = """
                name,code
                John,C1
                
                Jane,C2
                """;

        List<ParsedRow> rows = parser.parse(toStream(csv));
        assertEquals(2, rows.size());
    }

    @Test
    void emptyFile_returnsEmptyList() throws IOException {
        List<ParsedRow> rows = parser.parse(toStream(""));
        assertTrue(rows.isEmpty());
    }

    @Test
    void headerOnly_returnsEmptyList() throws IOException {
        List<ParsedRow> rows = parser.parse(toStream("name,code\n"));
        assertTrue(rows.isEmpty());
    }

    @Test
    void rowNumber_isFileLineNumber() throws IOException {
        String csv = """
                name,code
                John,C1
                Jane,C2
                """;

        List<ParsedRow> rows = parser.parse(toStream(csv));
        assertEquals(2, rows.get(0).rowNumber()); // data starts at line 2
        assertEquals(3, rows.get(1).rowNumber());
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
