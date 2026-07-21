package com.outreach.platform.ai.service;

import com.outreach.platform.ai.exception.AiProviderUnavailableException;
import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.AnomalyRequest;
import com.outreach.platform.ai.model.QueryRequest;
import com.outreach.platform.ai.model.SummarizeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpenAiServiceImpl.
 * Tests service logic with mocked ChatClient and verifies fallback behavior.
 */
@ExtendWith(MockitoExtension.class)
class OpenAiServiceImplTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    @Mock
    private ResourceLoader resourceLoader;

    private OpenAiServiceImpl openAiService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        openAiService = new OpenAiServiceImpl(chatClientBuilder, resourceLoader);
    }

    @Test
    void summarize_success_returnsAiJobResult() throws Exception {
        Resource promptResource = new ByteArrayResource(
                "Summarize: {{context}} (max {{maxLength}} words)".getBytes()
        );
        when(resourceLoader.getResource("classpath:prompts/summarize.txt")).thenReturn(promptResource);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Generated summary content");

        SummarizeRequest request = new SummarizeRequest("feedback data from event", 500);
        CompletableFuture<AiJobResult> future = openAiService.summarize(request);
        AiJobResult result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("Generated summary content");
    }

    @Test
    void detectAnomalies_success_returnsAiJobResult() throws Exception {
        Resource promptResource = new ByteArrayResource(
                "Detect anomalies in: {{dataset}} threshold={{threshold}}".getBytes()
        );
        when(resourceLoader.getResource("classpath:prompts/anomaly-detection.txt")).thenReturn(promptResource);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("No anomalies found");

        AnomalyRequest request = new AnomalyRequest(
                List.of(Map.of("score", 4), Map.of("score", 5)), 0.5
        );
        CompletableFuture<AiJobResult> future = openAiService.detectAnomalies(request);
        AiJobResult result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("No anomalies found");
    }

    @Test
    void query_success_returnsAiJobResult() throws Exception {
        Resource promptResource = new ByteArrayResource(
                "Query: {{prompt}} Context: {{context}}".getBytes()
        );
        when(resourceLoader.getResource("classpath:prompts/natural-language-query.txt")).thenReturn(promptResource);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Query result data");

        QueryRequest request = new QueryRequest("show top events", "context here");
        CompletableFuture<AiJobResult> future = openAiService.query(request);
        AiJobResult result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("Query result data");
    }

    @Test
    void summarize_whenTemplateLoadFails_returnsFailureResult() throws Exception {
        Resource badResource = mock(Resource.class);
        when(badResource.getContentAsString(any())).thenThrow(new java.io.IOException("File not found"));
        when(resourceLoader.getResource("classpath:prompts/summarize.txt")).thenReturn(badResource);

        SummarizeRequest request = new SummarizeRequest("data", null);
        CompletableFuture<AiJobResult> future = openAiService.summarize(request);
        AiJobResult result = future.get();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Failed to load prompt template");
    }

    @Test
    void summarizeFallback_throwsAiProviderUnavailableException() throws Exception {
        // Invoke the fallback method directly via reflection to test its behavior
        var fallbackMethod = OpenAiServiceImpl.class.getDeclaredMethod(
                "summarizeFallback", SummarizeRequest.class, Throwable.class
        );
        fallbackMethod.setAccessible(true);

        SummarizeRequest request = new SummarizeRequest("data", null);
        RuntimeException cause = new RuntimeException("Connection timeout");

        assertThatThrownBy(() -> fallbackMethod.invoke(openAiService, request, cause))
                .hasCauseInstanceOf(AiProviderUnavailableException.class);
    }
}
