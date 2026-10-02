package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.ParsedRow;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Every template the UI offers must be accepted by the importer as-is. */
class ImportTemplateServiceTest {

    private final ImportTemplateService templates = new ImportTemplateService();

    @ParameterizedTest
    @EnumSource(ImportTemplateService.Format.class)
    void templateRoundTripsThroughTheImporter_withOneValidExampleRow(ImportTemplateService.Format format) throws IOException {
        byte[] file = templates.render(format);

        List<ParsedRow> rows = format == ImportTemplateService.Format.CSV
                ? new CsvParser().parse(new ByteArrayInputStream(file))
                : new ExcelParser().parse(new ByteArrayInputStream(file), "." + format.name().toLowerCase());

        assertThat(rows).hasSize(1);
        assertThat(new RowValidator().validate(rows.getFirst())).isEmpty();
        assertThat(rows.getFirst().fields()).containsEntry("employeeid", "EMP1042");
    }
}
