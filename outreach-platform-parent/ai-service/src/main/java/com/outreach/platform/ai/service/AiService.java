package com.outreach.platform.ai.service;

import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.AnomalyRequest;
import com.outreach.platform.ai.model.QueryRequest;
import com.outreach.platform.ai.model.SummarizeRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for AI provider operations.
 * Implementations can target different providers (OpenAI, Bedrock, Ollama, mock)
 * and are selected via configuration property {@code platform.ai.provider}.
 * All operations are asynchronous, returning CompletableFuture for non-blocking execution.
 */
public interface AiService {

    /**
     * Summarize feedback text.
     *
     * @param request the summarization request containing feedback context
     * @return async result with AI-generated summary
     */
    CompletableFuture<AiJobResult> summarize(SummarizeRequest request);

    /**
     * Detect anomalies in feedback score datasets.
     *
     * @param request the anomaly detection request containing score data
     * @return async result with anomaly analysis
     */
    CompletableFuture<AiJobResult> detectAnomalies(AnomalyRequest request);

    /**
     * Execute a natural language query against feedback data.
     *
     * @param request the natural language query request
     * @return async result with query response
     */
    CompletableFuture<AiJobResult> query(QueryRequest request);
}
