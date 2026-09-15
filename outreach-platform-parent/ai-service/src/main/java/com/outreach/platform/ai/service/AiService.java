package com.outreach.platform.ai.service;

import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.AnomalyRequest;
import com.outreach.platform.ai.model.QueryRequest;
import com.outreach.platform.ai.model.SummarizeRequest;

import java.util.concurrent.CompletableFuture;

/** Abstraction layer for AI provider operations; all operations are async. */
public interface AiService {

    /** Summarize feedback text. */
    CompletableFuture<AiJobResult> summarize(SummarizeRequest request);

    /** Detect anomalies in feedback score datasets. */
    CompletableFuture<AiJobResult> detectAnomalies(AnomalyRequest request);

    /** Execute a natural language query against feedback data. */
    CompletableFuture<AiJobResult> query(QueryRequest request);
}
