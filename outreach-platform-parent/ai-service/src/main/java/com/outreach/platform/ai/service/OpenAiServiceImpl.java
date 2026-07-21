package com.outreach.platform.ai.service;

import com.outreach.platform.ai.exception.AiProviderUnavailableException;
import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.AnomalyRequest;
import com.outreach.platform.ai.model.QueryRequest;
import com.outreach.platform.ai.model.SummarizeRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * OpenAI-backed implementation of the AI service with resilience patterns.
 * Wraps Spring AI ChatClient calls with Resilience4j circuit breaker and retry.
 * Active by default when {@code platform.ai.provider=openai} (or when provider is not specified).
 */
@Service
@ConditionalOnProperty(name = "platform.ai.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiServiceImpl.class);

    private final ChatClient chatClient;
    private final ResourceLoader resourceLoader;

    @Inject
    public OpenAiServiceImpl(ChatClient.Builder chatClientBuilder, ResourceLoader resourceLoader) {
        this.chatClient = chatClientBuilder.build();
        this.resourceLoader = resourceLoader;
    }

    @Async
    @Override
    @CircuitBreaker(name = "aiProvider", fallbackMethod = "summarizeFallback")
    @Retry(name = "aiProvider")
    public CompletableFuture<AiJobResult> summarize(SummarizeRequest request) {
        log.info("Requesting summarization from OpenAI, context length: {}", request.context().length());
        try {
            String promptTemplate = loadPromptTemplate("summarize.txt");
            String prompt = promptTemplate
                    .replace("{{context}}", request.context())
                    .replace("{{maxLength}}", String.valueOf(
                            request.maxLength() != null ? request.maxLength() : 500));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return CompletableFuture.completedFuture(AiJobResult.success(response));
        } catch (IOException e) {
            log.error("Failed to load summarize prompt template", e);
            return CompletableFuture.completedFuture(AiJobResult.failure("Failed to load prompt template"));
        }
    }

    @Async
    @Override
    @CircuitBreaker(name = "aiProvider", fallbackMethod = "detectAnomaliesFallback")
    @Retry(name = "aiProvider")
    public CompletableFuture<AiJobResult> detectAnomalies(AnomalyRequest request) {
        log.info("Requesting anomaly detection from OpenAI, dataset size: {}", request.dataset().size());
        try {
            String promptTemplate = loadPromptTemplate("anomaly-detection.txt");
            String prompt = promptTemplate
                    .replace("{{dataset}}", request.dataset().toString())
                    .replace("{{threshold}}", String.valueOf(
                            request.threshold() != null ? request.threshold() : 0.5));

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return CompletableFuture.completedFuture(AiJobResult.success(response));
        } catch (IOException e) {
            log.error("Failed to load anomaly-detection prompt template", e);
            return CompletableFuture.completedFuture(AiJobResult.failure("Failed to load prompt template"));
        }
    }

    @Async
    @Override
    @CircuitBreaker(name = "aiProvider", fallbackMethod = "queryFallback")
    @Retry(name = "aiProvider")
    public CompletableFuture<AiJobResult> query(QueryRequest request) {
        log.info("Processing natural language query from OpenAI");
        try {
            String promptTemplate = loadPromptTemplate("natural-language-query.txt");
            String prompt = promptTemplate
                    .replace("{{prompt}}", request.prompt())
                    .replace("{{context}}", request.context() != null ? request.context() : "");

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return CompletableFuture.completedFuture(AiJobResult.success(response));
        } catch (IOException e) {
            log.error("Failed to load query prompt template", e);
            return CompletableFuture.completedFuture(AiJobResult.failure("Failed to load prompt template"));
        }
    }

    @SuppressWarnings("unused")
    private CompletableFuture<AiJobResult> summarizeFallback(SummarizeRequest request, Throwable t) {
        return handleFallback("summarize", t);
    }

    @SuppressWarnings("unused")
    private CompletableFuture<AiJobResult> detectAnomaliesFallback(AnomalyRequest request, Throwable t) {
        return handleFallback("detectAnomalies", t);
    }

    @SuppressWarnings("unused")
    private CompletableFuture<AiJobResult> queryFallback(QueryRequest request, Throwable t) {
        return handleFallback("query", t);
    }

    private CompletableFuture<AiJobResult> handleFallback(String operation, Throwable t) {
        log.error("AI provider unavailable for operation '{}': {}", operation, t.getMessage());
        throw new AiProviderUnavailableException(
                "AI provider is currently unavailable. Operation: " + operation
        );
    }

    private String loadPromptTemplate(String templateName) throws IOException {
        var resource = resourceLoader.getResource("classpath:prompts/" + templateName);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
