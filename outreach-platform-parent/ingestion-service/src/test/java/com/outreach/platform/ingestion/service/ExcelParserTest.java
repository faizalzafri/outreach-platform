package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.ParsedRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelParserTest {

    private ExcelParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExcelParser();
    }

    @Test
    void parsesXlsxFile() throws IOException {
        byte[] xlsxData = createXlsx(
                new String[]{"employeeId", "fullName", "email"},
                new String[][]{
                        {"EMP001", "John Doe", "john@example.com"},
                        {"EMP002", "Jane Smith", "jane@example.com"}
                }
        );

        List<ParsedRow> rows = parser.parse(new ByteArrayInputStream(xlsxData), ".xlsx");

        assertEquals(2, rows.size());
        assertEquals("EMP001", rows.get(0).fields().get("employeeid"));
        assertEquals("jane@example.com", rows.get(1).fields().get("email"));
    }

    @Test
    void emptyWorkbook_returnsEmptyList() throws IOException {
        byte[] xlsxData = createXlsx(new String[]{}, new String[][]{});

        List<ParsedRow> rows = parser.parse(new ByteArrayInputStream(xlsxData), ".xlsx");
        assertTrue(rows.isEmpty());
    }

    @Test
    void numericCells_convertedToString() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("score");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(12345);
            data.createCell(1).setCellValue(98.5);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            List<ParsedRow> rows = parser.parse(new ByteArrayInputStream(out.toByteArray()), ".xlsx");
            assertEquals("12345", rows.get(0).fields().get("id"));
            assertEquals("98.5", rows.get(0).fields().get("score"));
        }
    }

    @Test
    void skipsEmptyRows() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("name");

            // row 1: data
            sheet.createRow(1).createCell(0).setCellValue("John");
            // row 2: empty (created but no content)
            sheet.createRow(2);
            // row 3: data
            sheet.createRow(3).createCell(0).setCellValue("Jane");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            List<ParsedRow> rows = parser.parse(new ByteArrayInputStream(out.toByteArray()), ".xlsx");
            assertEquals(2, rows.size());
        }
    }

    private byte[] createXlsx(String[] headers, String[][] data) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();

            if (headers.length > 0) {
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
            }

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    row.createCell(c).setCellValue(data[r][c]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
