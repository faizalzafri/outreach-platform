package com.outreach.platform.ai.controller;

import com.outreach.platform.ai.config.AiServiceProperties;
import com.outreach.platform.ai.config.AiServiceProperties.FeatureToggle;
import com.outreach.platform.ai.config.AiServiceProperties.Features;
import com.outreach.platform.ai.entity.AiJobDocument;
import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.AiJobStatus;
import com.outreach.platform.ai.model.AiJobType;
import com.outreach.platform.ai.model.SummarizeRequest;
import com.outreach.platform.ai.repo.AiJobRepository;
import com.outreach.platform.ai.service.AiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying the async AI job lifecycle:
 * job submission (202), async completion triggering, and result retrieval.
 * Uses WebMvcTest with mocked dependencies to test controller flow.
 */
@WebMvcTest(AiController.class)
@Import(AiJobLifecycleIT.TestConfig.class)
class AiJobLifecycleIT {

    @TestConfiguration
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("ADMIN"));
            return http.build();
        }

        @Bean
        public AiServiceProperties aiServiceProperties() {
            return new AiServiceProperties(
                    "mock", "test-key", "gpt-4o-mini", 4096, 0.7, 24,
                    new Features(
                            new FeatureToggle(true),
                            new FeatureToggle(true),
                            new FeatureToggle(true)
                    )
            );
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private AiJobRepository aiJobRepository;

    @Test
    @DisplayName("POST /ai/summarize returns 202 Accepted with jobId")
    @WithMockUser(roles = "ADMIN")
    void summarize_returns202WithJobId() throws Exception {
        AiJobDocument savedJob = createPendingJob("job-123");
        when(aiJobRepository.save(any())).thenReturn(savedJob);
        when(aiService.summarize(any(SummarizeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AiJobResult.success("mock summary")));

        String requestBody = """
                {
                    "context": "Team collaboration improved significantly this quarter."
                }
                """;

        mockMvc.perform(post("/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Job submitted successfully"));

        // Verify async AI service was called
        verify(aiService).summarize(any(SummarizeRequest.class));
    }

    @Test
    @DisplayName("GET /ai/jobs/{jobId} returns completed result after async processing")
    @WithMockUser(roles = "ADMIN")
    void getJobResult_returnsCompletedJob() throws Exception {
        // Simulate a completed job in the repository
        AiJobDocument completedJob = createPendingJob("job-456");
        completedJob.setStatus(AiJobStatus.COMPLETED);
        completedJob.setResult("This is a mock summary of the feedback.");
        completedJob.setCompletedAt(Instant.now());

        when(aiJobRepository.findById("job-456")).thenReturn(Optional.of(completedJob));

        mockMvc.perform(get("/ai/jobs/job-456")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-456"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result").value("This is a mock summary of the feedback."));
    }

    @Test
    @DisplayName("GET /ai/jobs/{jobId} returns 404 for non-existent job")
    @WithMockUser(roles = "ADMIN")
    void getJobResult_returns404_whenJobNotFound() throws Exception {
        when(aiJobRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/ai/jobs/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /ai/status returns capabilities with enabled features")
    @WithMockUser(roles = "ADMIN")
    void status_returnsCapabilities() throws Exception {
        mockMvc.perform(get("/ai/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.features.summarize").value(true))
                .andExpect(jsonPath("$.features.anomalies").value(true))
                .andExpect(jsonPath("$.features.query").value(true));
    }

    @Test
    @DisplayName("POST /ai/summarize triggers async service call and persists job")
    @WithMockUser(roles = "ADMIN")
    void summarize_triggersAsyncProcessingAndPersistsJob() throws Exception {
        AiJobDocument savedJob = createPendingJob("job-789");
        ArgumentCaptor<AiJobDocument> jobCaptor = ArgumentCaptor.forClass(AiJobDocument.class);
        when(aiJobRepository.save(jobCaptor.capture())).thenReturn(savedJob);
        when(aiService.summarize(any(SummarizeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AiJobResult.success("summary result")));

        String requestBody = """
                {
                    "context": "Test context for persistence verification.",
                    "maxLength": 200
                }
                """;

        mockMvc.perform(post("/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted());

        // Two save() calls happen here now: job creation (PENDING), then completeJob's save once
        // the (already-completed, in this test) future resolves — so assert against the first
        // captured value, the state at creation time, not getValue()'s last-call semantics.
        AiJobDocument capturedJob = jobCaptor.getAllValues().get(0);
        assertThat(capturedJob.getJobType()).isEqualTo(AiJobType.SUMMARIZE);
        assertThat(capturedJob.getStatus()).isEqualTo(AiJobStatus.PENDING);
        assertThat(capturedJob.getRequest()).containsKey("context");
    }

    private AiJobDocument createPendingJob(String jobId) {
        AiJobDocument job = new AiJobDocument(
                java.util.UUID.randomUUID(),
                AiJobType.SUMMARIZE,
                Map.of("context", "test", "maxLength", 500),
                Instant.now().plusSeconds(86400)
        );
        job.setId(jobId);
        return job;
    }
}
