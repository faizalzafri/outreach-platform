package com.outreach.platform.ingestion.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Volunteer import templates in every format the importer accepts. Each has the exact headers
 * {@link RowValidator} reads and one example row that passes validation; the Excel versions add an
 * Instructions sheet describing every column.
 */
@Service
public class ImportTemplateService {

    public enum Format {
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        XLS("xls", "application/vnd.ms-excel"),
        CSV("csv", "text/csv");

        private final String extension;
        private final String contentType;

        Format(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public String fileName() {
            return "volunteer-import-template." + extension;
        }

        public String contentType() {
            return contentType;
        }

        public static Format of(String value) {
            try {
                return valueOf((value == null ? "xlsx" : value.trim()).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported template format '" + value + "'; use xlsx, xls or csv");
            }
        }
    }

    /** Column, whether it is required, what it holds, and the example value. */
    private record Column(String header, boolean required, String description, String example) {
    }

    private static final List<Column> COLUMNS = List.of(
            new Column("employeeId", true, "Letters and digits only, up to 50 characters", "EMP1042"),
            new Column("fullName", true, "Up to 255 characters", "Kavya Rao"),
            new Column("email", true, "A valid email address", "kavya.rao@example.com"),
            new Column("phone", false, "Digits, spaces and hyphens only", "98765 43210"),
            new Column("baseLocation", false, "City or office", "Bangalore"),
            new Column("department", false, "", "Engineering"),
            new Column("designation", false, "", "Senior Engineer"),
            new Column("skills", false, "Comma-separated", "Teaching, First aid"),
            new Column("eventCode", true, "Code of the event to enrol the volunteer in (shown on the event page)",
                    "EVT-2024-001"));

    public byte[] render(Format format) {
        return switch (format) {
            case CSV -> csv();
            case XLSX -> excel(new XSSFWorkbook());
            case XLS -> excel(new HSSFWorkbook());
        };
    }

    private static byte[] csv() {
        String header = COLUMNS.stream().map(Column::header).collect(Collectors.joining(","));
        String example = COLUMNS.stream().map(c -> quote(c.example())).collect(Collectors.joining(","));
        // UTF-8 BOM so Excel opens non-ASCII names correctly
        return ("﻿" + header + "\r\n" + example + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private static byte[] excel(Workbook workbook) {
        try (workbook) {
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);

            Sheet volunteers = workbook.createSheet("Volunteers");
            Row header = volunteers.createRow(0);
            Row example = volunteers.createRow(1);
            for (int i = 0; i < COLUMNS.size(); i++) {
                header.createCell(i).setCellValue(COLUMNS.get(i).header());
                header.getCell(i).setCellStyle(bold);
                example.createCell(i).setCellValue(COLUMNS.get(i).example());
                volunteers.setColumnWidth(i, 22 * 256);
            }

            Sheet help = workbook.createSheet("Instructions");
            help.createRow(0).createCell(0).setCellValue(
                    "Fill the Volunteers sheet from row 2 (replace the example). Keep the header row as it is.");
            Row helpHeader = help.createRow(2);
            String[] titles = {"Column", "Required", "Format"};
            for (int i = 0; i < titles.length; i++) {
                helpHeader.createCell(i).setCellValue(titles[i]);
                helpHeader.getCell(i).setCellStyle(bold);
            }
            for (int i = 0; i < COLUMNS.size(); i++) {
                Row row = help.createRow(3 + i);
                row.createCell(0).setCellValue(COLUMNS.get(i).header());
                row.createCell(1).setCellValue(COLUMNS.get(i).required() ? "Yes" : "No");
                row.createCell(2).setCellValue(COLUMNS.get(i).description());
            }
            help.setColumnWidth(0, 18 * 256);
            help.setColumnWidth(2, 70 * 256);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
