package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.ParsedRow;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses CSV files into a list of parsed rows, handling quoted fields. */
@Named
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);
    private static final char DELIMITER = ',';
    private static final char QUOTE = '"';

    /** Parse a CSV input stream into rows with column-to-value mappings. */
    public List<ParsedRow> parse(InputStream inputStream) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return List.of();
            }

            List<String> headers = parseLine(headerLine).stream()
                    .map(h -> h.trim().toLowerCase())
                    .toList();

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseLine(line);
                Map<String, String> fields = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = i < values.size() ? values.get(i).trim() : "";
                    fields.put(headers.get(i), value);
                }
                rows.add(new ParsedRow(rowNumber, fields));
            }
        }
        return rows;
    }


    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (inQuotes) {
                if (ch == QUOTE) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                        current.append(QUOTE);
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else {
                if (ch == QUOTE) {
                    inQuotes = true;
                } else if (ch == DELIMITER) {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(ch);
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
