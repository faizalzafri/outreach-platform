package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.ParsedRow;
import jakarta.inject.Named;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses Excel files (.xlsx and .xls) into a list of parsed rows using Apache POI. */
@Named
public class ExcelParser {

    private static final Logger log = LoggerFactory.getLogger(ExcelParser.class);

    /** Parse an Excel input stream into rows with column-to-value mappings. */
    public List<ParsedRow> parse(InputStream inputStream, String extension) throws IOException {
        try (Workbook workbook = createWorkbook(inputStream, extension)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = extractHeaders(sheet);
            if (headers.isEmpty()) {
                return List.of();
            }
            return extractRows(sheet, headers);
        }
    }

    private Workbook createWorkbook(InputStream inputStream, String extension) throws IOException {
        if (".xls".equalsIgnoreCase(extension)) {
            return new HSSFWorkbook(inputStream);
        }
        return new XSSFWorkbook(inputStream);
    }

    private List<String> extractHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return List.of();
        }
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String value = getCellValueAsString(cell).trim().toLowerCase();
            if (!value.isEmpty()) {
                headers.add(value);
            }
        }
        return headers;
    }

    private List<ParsedRow> extractRows(Sheet sheet, List<String> headers) {
        List<ParsedRow> rows = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();

        for (int rowIdx = 1; rowIdx <= lastRow; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null || isRowEmpty(row)) {
                continue;
            }
            Map<String, String> fields = new LinkedHashMap<>();
            for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                fields.put(headers.get(colIdx), getCellValueAsString(cell).trim());
            }
            rows.add(new ParsedRow(rowIdx + 1, fields)); // 1-based row number (header is row 1)
        }
        return rows;
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numericValue = cell.getNumericCellValue();
                if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                    yield String.valueOf((long) numericValue);
                }
                yield String.valueOf(numericValue);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BLANK, ERROR -> "";
            default -> "";
        };
    }
}
