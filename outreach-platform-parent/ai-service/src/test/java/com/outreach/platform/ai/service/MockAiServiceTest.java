package com.outreach.platform.ai.service;

import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.AnomalyRequest;
import com.outreach.platform.ai.model.QueryRequest;
import com.outreach.platform.ai.model.SummarizeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the mock AI service implementation.
 * Verifies canned responses are returned correctly for all operations.
 */
class MockAiServiceTest {

    private MockAiService mockAiService;

    @BeforeEach
    void setUp() {
        mockAiService = new MockAiService();
    }

    @Test
    void summarize_returnsCannedSummaryResponse() throws Exception {
        SummarizeRequest request = new SummarizeRequest("Some feedback text to summarize", 500);

        CompletableFuture<AiJobResult> future = mockAiService.summarize(request);
        AiJobResult result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.content()).contains("mock summary");
        assertThat(result.content()).contains("positive");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void detectAnomalies_returnsCannedAnomalyResponse() throws Exception {
        AnomalyRequest request = new AnomalyRequest(
                List.of(Map.of("score", 4), Map.of("score", 5)),
                0.5
        );

        CompletableFuture<AiJobResult> future = mockAiService.detectAnomalies(request);
        AiJobResult result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.content()).contains("No anomalies detected");
        assertThat(result.content()).contains("2 data points");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void query_returnsCannedQueryResponse() throws Exception {
        QueryRequest request = new QueryRequest("What are the top activities?", null);

        CompletableFuture<AiJobResult> future = mockAiService.query(request);
        AiJobResult result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.content()).contains("Mock query result");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void summarize_simulatesLatency() throws Exception {
        SummarizeRequest request = new SummarizeRequest("data", null);

        long start = System.currentTimeMillis();
        mockAiService.summarize(request).get();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(400L);
    }

    @Test
    void allOperations_returnSuccessResults() throws Exception {
        SummarizeRequest summarizeReq = new SummarizeRequest("text", 100);
        AnomalyRequest anomalyReq = new AnomalyRequest(List.of(Map.of("v", 1)), null);
        QueryRequest queryReq = new QueryRequest("question", "ctx");

        assertThat(mockAiService.summarize(summarizeReq).get().success()).isTrue();
        assertThat(mockAiService.detectAnomalies(anomalyReq).get().success()).isTrue();
        assertThat(mockAiService.query(queryReq).get().success()).isTrue();
    }
}
