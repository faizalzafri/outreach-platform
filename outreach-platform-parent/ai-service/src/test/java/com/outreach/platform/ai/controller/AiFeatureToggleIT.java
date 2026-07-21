package com.outreach.platform.ai.controller;

import com.outreach.platform.ai.config.AiServiceProperties;
import com.outreach.platform.ai.config.AiServiceProperties.FeatureToggle;
import com.outreach.platform.ai.config.AiServiceProperties.Features;
import com.outreach.platform.ai.repo.AiJobRepository;
import com.outreach.platform.ai.service.AiService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying that disabled AI features return 501 NOT_IMPLEMENTED.
 * Uses WebMvcTest slice — no MongoDB needed since requests are rejected at the toggle check.
 */
@WebMvcTest(AiController.class)
@Import(AiFeatureToggleIT.TestConfig.class)
class AiFeatureToggleIT {

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
                            new FeatureToggle(false),
                            new FeatureToggle(false),
                            new FeatureToggle(false)
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
    @DisplayName("POST /ai/summarize returns 501 when summarize feature is disabled")
    @WithMockUser(roles = "ADMIN")
    void summarize_returnsNotImplemented_whenFeatureDisabled() throws Exception {
        String requestBody = """
                {
                    "context": "Some feedback text to summarize"
                }
                """;

        mockMvc.perform(post("/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("AI_FEATURE_DISABLED"))
                .andExpect(jsonPath("$.feature").value("summarize"));
    }

    @Test
    @DisplayName("POST /ai/anomalies returns 501 when anomalies feature is disabled")
    @WithMockUser(roles = "ADMIN")
    void anomalies_returnsNotImplemented_whenFeatureDisabled() throws Exception {
        String requestBody = """
                {
                    "dataset": [{"score": 4.5}, {"score": 3.2}]
                }
                """;

        mockMvc.perform(post("/ai/anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("AI_FEATURE_DISABLED"))
                .andExpect(jsonPath("$.feature").value("anomalies"));
    }

    @Test
    @DisplayName("POST /ai/query returns 501 when query feature is disabled")
    @WithMockUser(roles = "ADMIN")
    void query_returnsNotImplemented_whenFeatureDisabled() throws Exception {
        String requestBody = """
                {
                    "prompt": "What is the average satisfaction score?"
                }
                """;

        mockMvc.perform(post("/ai/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("AI_FEATURE_DISABLED"))
                .andExpect(jsonPath("$.feature").value("query"));
    }
}
