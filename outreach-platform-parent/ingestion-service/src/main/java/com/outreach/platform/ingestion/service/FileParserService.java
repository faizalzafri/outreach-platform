package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.config.IngestionServiceProperties;
import com.outreach.platform.ingestion.model.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates file parsing by delegating to the appropriate parser
 * based on file extension, then validates all rows.
 */
@Named
public class FileParserService {

    private static final Logger log = LoggerFactory.getLogger(FileParserService.class);

    private final ExcelParser excelParser;
    private final CsvParser csvParser;
    private final RowValidator rowValidator;
    private final IngestionServiceProperties properties;

    @Inject
    public FileParserService(ExcelParser excelParser,
                             CsvParser csvParser,
                             RowValidator rowValidator,
                             IngestionServiceProperties properties) {
        this.excelParser = excelParser;
        this.csvParser = csvParser;
        this.rowValidator = rowValidator;
        this.properties = properties;
    }

    /**
     * Parse and validate a multipart file. Returns structured results with valid rows and errors.
     *
     * @param file the uploaded multipart file
     * @return parse result containing valid rows, errors, and counts
     * @throws IOException if the file cannot be read
     */
    public ParseResult parseAndValidate(MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        validateExtension(extension);

        List<ParsedRow> parsedRows = parseFile(file.getInputStream(), extension);

        List<ParsedRow> validRows = new ArrayList<>();
        List<ValidationError> allErrors = new ArrayList<>();

        for (ParsedRow row : parsedRows) {
            List<ValidationError> rowErrors = rowValidator.validate(row);
            if (rowErrors.isEmpty()) {
                validRows.add(row);
            } else {
                allErrors.addAll(rowErrors);
            }
        }

        return new ParseResult(
                validRows,
                allErrors,
                parsedRows.size(),
                validRows.size(),
                parsedRows.size() - validRows.size()
        );
    }

    /**
     * Validates the file extension against the configured allowed list.
     *
     * @param extension the file extension including the dot
     * @throws IllegalArgumentException if the extension is not allowed
     */
    public void validateExtension(String extension) {
        if (!properties.allowedExtensions().contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file extension: " + extension +
                            ". Allowed: " + properties.allowedExtensions());
        }
    }

    /**
     * Extracts the file extension from a filename.
     */
    public String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    private List<ParsedRow> parseFile(InputStream inputStream, String extension) throws IOException {
        return switch (extension) {
            case ".xlsx", ".xls" -> excelParser.parse(inputStream, extension);
            case ".csv" -> csvParser.parse(inputStream);
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };
    }
}
