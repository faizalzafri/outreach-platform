package com.outreach.platform.ai.controller;

import com.outreach.platform.ai.config.AiServiceProperties;
import com.outreach.platform.ai.config.AiServiceProperties.FeatureToggle;
import com.outreach.platform.ai.config.AiServiceProperties.Features;
import com.outreach.platform.ai.entity.AiJobDocument;
import com.outreach.platform.ai.model.AiJobType;
import com.outreach.platform.ai.repo.AiJobRepository;
import com.outreach.platform.ai.service.AiService;
import com.outreach.platform.ai.model.AiJobResult;
import com.outreach.platform.ai.model.SummarizeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying role-based access control for AI endpoints.
 * Only ROLE_ADMIN is allowed; PMO and POC roles are rejected.
 */
@WebMvcTest(AiController.class)
@Import(AiSecurityIT.TestConfig.class)
class AiSecurityIT {

    @TestConfiguration
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                    .anyRequest().hasRole("ADMIN")
                )
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(401, "Unauthorized"))
                );
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

    private static final String SUMMARIZE_REQUEST = """
            {
                "context": "Feedback data for security test"
            }
            """;

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(post("/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUMMARIZE_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated user with ROLE_PMO returns 403 Forbidden")
    @WithMockUser(roles = "PMO")
    void pmoRole_returns403() throws Exception {
        mockMvc.perform(post("/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUMMARIZE_REQUEST))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated user with ROLE_POC returns 403 Forbidden")
    @WithMockUser(roles = "POC")
    void pocRole_returns403() throws Exception {
        mockMvc.perform(post("/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUMMARIZE_REQUEST))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated user with ROLE_ADMIN returns 202 Accepted")
    @WithMockUser(roles = "ADMIN")
    void adminRole_returns202() throws Exception {
        // Mock repository save to return a document with an ID
        AiJobDocument savedJob = new AiJobDocument(AiJobType.SUMMARIZE, Map.of("context", "test"), Instant.now().plusSeconds(3600));
        savedJob.setId("test-job-id");
        when(aiJobRepository.save(any())).thenReturn(savedJob);
        when(aiService.summarize(any(SummarizeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AiJobResult.success("mock result")));

        mockMvc.perform(post("/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUMMARIZE_REQUEST))
                .andExpect(status().isAccepted());
    }
}
