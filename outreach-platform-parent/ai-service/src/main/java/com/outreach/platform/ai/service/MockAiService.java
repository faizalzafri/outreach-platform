package com.outreach.platform.ai.service;

import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.AnomalyRequest;
import com.outreach.platform.ai.model.QueryRequest;
import com.outreach.platform.ai.model.SummarizeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/** Mock AI provider returning canned responses for testing and development. */
@Service
@ConditionalOnProperty(name = "platform.ai.provider", havingValue = "mock")
public class MockAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(MockAiService.class);
    private static final long SIMULATED_LATENCY_MS = 500;

    @Async
    @Override
    public CompletableFuture<AiJobResult> summarize(SummarizeRequest request) {
        log.info("Mock AI: generating summary for context length {}", request.context().length());
        simulateLatency();
        String content = "This is a mock summary of the feedback. "
                + "Overall sentiment is positive with 85% satisfaction rate. "
                + "Key themes include teamwork, community impact, and personal growth.";
        return CompletableFuture.completedFuture(AiJobResult.success(content));
    }

    @Async
    @Override
    public CompletableFuture<AiJobResult> detectAnomalies(AnomalyRequest request) {
        log.info("Mock AI: detecting anomalies in dataset of {} items", request.dataset().size());
        simulateLatency();
        String content = "No anomalies detected in the feedback scores. "
                + "All " + request.dataset().size() + " data points fall within expected ranges.";
        return CompletableFuture.completedFuture(AiJobResult.success(content));
    }

    @Async
    @Override
    public CompletableFuture<AiJobResult> query(QueryRequest request) {
        log.info("Mock AI: processing query '{}'", request.prompt());
        simulateLatency();
        String content = "Mock query result: Based on the feedback data, the top-rated activities "
                + "are community outreach and mentoring programs. Average satisfaction score is 4.2/5.";
        return CompletableFuture.completedFuture(AiJobResult.success(content));
    }

    private void simulateLatency() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
