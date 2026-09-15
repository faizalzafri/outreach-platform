package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.client.EventServiceClient;
import com.outreach.platform.ingestion.model.ParseResult;
import com.outreach.platform.ingestion.model.ParsedRow;
import com.outreach.platform.ingestion.model.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportProcessingService Unit Tests")
class ImportProcessingServiceTest {

    @Mock
    private FileParserService fileParserService;
    @Mock
    private JobTrackingService jobTrackingService;
    @Mock
    private EventServiceClient eventServiceClient;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    private ImportProcessingService importProcessingService;

    @BeforeEach
    void setUp() {
        importProcessingService = new ImportProcessingService(
                fileParserService, jobTrackingService, eventServiceClient, domainEventPublisher);
    }

    private ParsedRow buildRow(int rowNumber, String employeeId, String eventCode) {
        return new ParsedRow(rowNumber, Map.of(
                "employeeid", employeeId,
                "fullname", "Volunteer " + employeeId,
                "email", employeeId.toLowerCase() + "@example.com",
                "phone", "9876543210",
                "baselocation", "Pune",
                "department", "Engineering",
                "designation", "Developer",
                "skills", "Java",
                "eventcode", eventCode
        ));
    }

    @Test
    @DisplayName("all valid rows for the same event succeed and are grouped into one VolunteersImported event")
    void allRowsSucceed_groupedByEvent() throws IOException {
        UUID eventId = UUID.randomUUID();
        ParsedRow row1 = buildRow(1, "EMP001", "EVT-001");
        ParsedRow row2 = buildRow(2, "EMP002", "EVT-001");

        when(fileParserService.parseAndValidate(any(InputStream.class), eq(".csv")))
                .thenReturn(new ParseResult(List.of(row1, row2), List.of(), 2, 2, 0));
        when(eventServiceClient.importVolunteer(any()))
                .thenReturn(new EventServiceClient.VolunteerImportResponse(UUID.randomUUID(), eventId, false));

        importProcessingService.processImport("job-1", "content".getBytes(), "volunteers.csv", ".csv");

        verify(jobTrackingService).startJob("job-1", 2);
        verify(jobTrackingService, times(2)).updateProgress(eq("job-1"), anyInt(), eq(2));
        verify(jobTrackingService).completeJob("job-1", 2, 0, List.of());

        ArgumentCaptor<List<Map<String, String>>> volunteersCaptor = ArgumentCaptor.forClass(List.class);
        verify(domainEventPublisher).publishVolunteersImported(eq(eventId), volunteersCaptor.capture());
        assertThat(volunteersCaptor.getValue()).hasSize(2);

        verify(domainEventPublisher).publishImportJobCompleted("job-1", "volunteers.csv", "COMPLETED", 2, 2, 0);
    }

    @Test
    @DisplayName("rows spanning different events publish one VolunteersImported per event")
    void rowsAcrossDifferentEvents_publishOnePerEvent() throws IOException {
        UUID eventA = UUID.randomUUID();
        UUID eventB = UUID.randomUUID();
        ParsedRow row1 = buildRow(1, "EMP001", "EVT-A");
        ParsedRow row2 = buildRow(2, "EMP002", "EVT-B");

        when(fileParserService.parseAndValidate(any(InputStream.class), eq(".csv")))
                .thenReturn(new ParseResult(List.of(row1, row2), List.of(), 2, 2, 0));
        when(eventServiceClient.importVolunteer(argThatEmployeeId("EMP001")))
                .thenReturn(new EventServiceClient.VolunteerImportResponse(UUID.randomUUID(), eventA, false));
        when(eventServiceClient.importVolunteer(argThatEmployeeId("EMP002")))
                .thenReturn(new EventServiceClient.VolunteerImportResponse(UUID.randomUUID(), eventB, false));

        importProcessingService.processImport("job-2", "content".getBytes(), "volunteers.csv", ".csv");

        verify(domainEventPublisher).publishVolunteersImported(eq(eventA), any());
        verify(domainEventPublisher).publishVolunteersImported(eq(eventB), any());
        verify(jobTrackingService).completeJob("job-2", 2, 0, List.of());
    }

    @Test
    @DisplayName("a failure on one row is recorded as an error and does not stop other rows")
    void oneRowFails_othersStillProcessed() throws IOException {
        UUID eventId = UUID.randomUUID();
        ParsedRow goodRow = buildRow(1, "EMP001", "EVT-001");
        ParsedRow badRow = buildRow(2, "EMP002", "EVT-UNKNOWN");

        when(fileParserService.parseAndValidate(any(InputStream.class), eq(".csv")))
                .thenReturn(new ParseResult(List.of(goodRow, badRow), List.of(), 2, 2, 0));
        when(eventServiceClient.importVolunteer(argThatEmployeeId("EMP001")))
                .thenReturn(new EventServiceClient.VolunteerImportResponse(UUID.randomUUID(), eventId, false));
        when(eventServiceClient.importVolunteer(argThatEmployeeId("EMP002")))
                .thenThrow(new RuntimeException("Event not found for code: EVT-UNKNOWN"));

        importProcessingService.processImport("job-3", "content".getBytes(), "volunteers.csv", ".csv");

        verify(jobTrackingService).completeJob(eq("job-3"), eq(1), eq(1),
                org.mockito.ArgumentMatchers.argThat(errors -> errors.size() == 1 && errors.get(0).rowNumber() == 2));
        verify(domainEventPublisher).publishVolunteersImported(eq(eventId), any());
    }

    @Test
    @DisplayName("schema validation errors from parsing are included in the final error list")
    void parseValidationErrors_areIncludedInJobErrors() throws IOException {
        ParsedRow goodRow = buildRow(1, "EMP001", "EVT-001");
        ValidationError parseError = new ValidationError(2, "email", "Invalid email format", "not-an-email");

        when(fileParserService.parseAndValidate(any(InputStream.class), eq(".csv")))
                .thenReturn(new ParseResult(List.of(goodRow), List.of(parseError), 2, 1, 1));
        when(eventServiceClient.importVolunteer(any()))
                .thenReturn(new EventServiceClient.VolunteerImportResponse(UUID.randomUUID(), UUID.randomUUID(), false));

        importProcessingService.processImport("job-4", "content".getBytes(), "volunteers.csv", ".csv");

        verify(jobTrackingService).completeJob(eq("job-4"), eq(1), eq(1), eq(List.of(parseError)));
    }

    @Test
    @DisplayName("already-enrolled volunteers are not treated as errors")
    void alreadyEnrolled_isNotAnError() throws IOException {
        UUID eventId = UUID.randomUUID();
        ParsedRow row = buildRow(1, "EMP001", "EVT-001");

        when(fileParserService.parseAndValidate(any(InputStream.class), eq(".csv")))
                .thenReturn(new ParseResult(List.of(row), List.of(), 1, 1, 0));
        when(eventServiceClient.importVolunteer(any()))
                .thenReturn(new EventServiceClient.VolunteerImportResponse(UUID.randomUUID(), eventId, true));

        importProcessingService.processImport("job-5", "content".getBytes(), "volunteers.csv", ".csv");

        verify(jobTrackingService).completeJob("job-5", 1, 0, List.of());
        verify(domainEventPublisher).publishVolunteersImported(eq(eventId), any());
    }

    @Test
    @DisplayName("a whole-file parse failure marks the job FAILED and does not throw")
    void wholeFileParseFailure_marksJobFailed() throws IOException {
        when(fileParserService.parseAndValidate(any(InputStream.class), eq(".csv")))
                .thenThrow(new IOException("corrupt file"));

        importProcessingService.processImport("job-6", "content".getBytes(), "volunteers.csv", ".csv");

        verify(jobTrackingService).failJob(eq("job-6"), anyString());
        verify(jobTrackingService, never()).completeJob(anyString(), anyInt(), anyInt(), any());
        verify(domainEventPublisher).publishImportJobCompleted("job-6", "volunteers.csv", "FAILED", 0, 0, 0);
    }

    private EventServiceClient.VolunteerImportRequest argThatEmployeeId(String employeeId) {
        return org.mockito.ArgumentMatchers.argThat(req -> req != null && employeeId.equals(req.employeeId()));
    }
}
