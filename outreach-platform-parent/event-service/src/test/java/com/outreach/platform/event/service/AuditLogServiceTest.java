package com.outreach.platform.event.service;

import com.outreach.platform.event.model.AuditLogDocument;
import com.outreach.platform.event.model.dto.AuditLogEntry;
import com.outreach.platform.event.model.dto.AuditLogSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Unit Tests")
class AuditLogServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(mongoTemplate);
    }

    @Test
    @DisplayName("log() saves audit entry to audit_logs collection")
    void logShouldSaveToAuditLogsCollection() {
        Map<String, Object> details = Map.of("previousStatus", "DRAFT", "newStatus", "PUBLISHED");

        auditLogService.log("admin-user", "UPDATE_STATUS", "Event", "event-123", details);

        ArgumentCaptor<AuditLogDocument> captor = ArgumentCaptor.forClass(AuditLogDocument.class);
        verify(mongoTemplate).save(captor.capture(), eq("audit_logs"));

        AuditLogDocument saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("admin-user");
        assertThat(saved.getAction()).isEqualTo("UPDATE_STATUS");
        assertThat(saved.getResourceType()).isEqualTo("Event");
        assertThat(saved.getResourceId()).isEqualTo("event-123");
        assertThat(saved.getDetails()).isEqualTo(details);
        assertThat(saved.getTimestamp()).isNotNull();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("log() sets timestamp to current time")
    void logShouldSetTimestampToCurrentTime() {
        Instant before = Instant.now();

        auditLogService.log("user1", "CREATE_EVENT", "Event", "evt-001", null);

        Instant after = Instant.now();

        ArgumentCaptor<AuditLogDocument> captor = ArgumentCaptor.forClass(AuditLogDocument.class);
        verify(mongoTemplate).save(captor.capture(), eq("audit_logs"));

        assertThat(captor.getValue().getTimestamp()).isBetween(before, after);
    }

    @Test
    @DisplayName("log() handles null details")
    void logShouldHandleNullDetails() {
        auditLogService.log("user1", "DELETE_EVENT", "Event", "evt-002", null);

        ArgumentCaptor<AuditLogDocument> captor = ArgumentCaptor.forClass(AuditLogDocument.class);
        verify(mongoTemplate).save(captor.capture(), eq("audit_logs"));

        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    @DisplayName("log() generates unique ID for each entry")
    void logShouldGenerateUniqueIds() {
        auditLogService.log("user1", "ACTION1", "Type1", "id1", null);
        auditLogService.log("user1", "ACTION2", "Type2", "id2", null);

        ArgumentCaptor<AuditLogDocument> captor = ArgumentCaptor.forClass(AuditLogDocument.class);
        verify(mongoTemplate, times(2)).save(captor.capture(), eq("audit_logs"));

        List<AuditLogDocument> saved = captor.getAllValues();
        assertThat(saved.get(0).getId()).isNotEqualTo(saved.get(1).getId());
    }

    @Test
    @DisplayName("queryAuditLogs() returns paginated results")
    void queryAuditLogsShouldReturnPaginatedResults() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(
                null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        AuditLogDocument doc = new AuditLogDocument("user1", "CREATE", "Event", "evt-1", null);
        when(mongoTemplate.count(any(Query.class), eq(AuditLogDocument.class), eq("audit_logs")))
                .thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(AuditLogDocument.class), eq("audit_logs")))
                .thenReturn(List.of(doc));

        Page<AuditLogEntry> result = auditLogService.queryAuditLogs(criteria, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo("user1");
        assertThat(result.getContent().get(0).action()).isEqualTo("CREATE");
    }

    @Test
    @DisplayName("exportAuditLogCsv() returns CSV with header and rows")
    void exportAuditLogCsvShouldReturnCsv() {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(
                null, null, null, null, null);
        AuditLogDocument doc = new AuditLogDocument("user1", "UPDATE", "Event", "evt-1", null);

        when(mongoTemplate.find(any(Query.class), eq(AuditLogDocument.class), eq("audit_logs")))
                .thenReturn(List.of(doc));

        String csv = auditLogService.exportAuditLogCsv(criteria);

        assertThat(csv).startsWith("id,userId,action,resourceType,resourceId,timestamp\n");
        assertThat(csv).contains("user1");
        assertThat(csv).contains("UPDATE");
        assertThat(csv).contains("Event");
        assertThat(csv).contains("evt-1");
    }
}
