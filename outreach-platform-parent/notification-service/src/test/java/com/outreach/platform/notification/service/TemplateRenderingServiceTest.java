package com.outreach.platform.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for TemplateRenderingService — verifies Thymeleaf
 * string-based template rendering with variable substitution.
 */
class TemplateRenderingServiceTest {

    private TemplateRenderingService renderingService;

    @BeforeEach
    void setUp() {
        renderingService = new TemplateRenderingService();
        renderingService.configureEngine();
    }

    @Test
    void rendersInlineExpressionWithSingleVariable() {
        String template = "Hello [[${volunteerName}]], welcome!";
        Map<String, Object> variables = Map.of("volunteerName", "Alice");

        String result = renderingService.render(template, variables);

        assertEquals("Hello Alice, welcome!", result);
    }

    @Test
    void rendersMultipleVariables() {
        String template = "<p>Event: [[${eventName}]] on [[${eventDate}]]</p>";
        Map<String, Object> variables = Map.of(
                "eventName", "Community Cleanup",
                "eventDate", "2025-03-15"
        );

        String result = renderingService.render(template, variables);

        assertEquals("<p>Event: Community Cleanup on 2025-03-15</p>", result);
    }

    @Test
    void rendersHtmlWithThExpressions() {
        String template = "<div><span th:text=\"${greeting}\">placeholder</span></div>";
        Map<String, Object> variables = Map.of("greeting", "Hi there!");

        String result = renderingService.render(template, variables);

        assertNotNull(result);
        assert result.contains("Hi there!");
    }

    @Test
    void handlesEmptyVariablesGracefully() {
        String template = "Static content with no variables";

        String result = renderingService.render(template, Map.of());

        assertEquals("Static content with no variables", result);
    }

    @Test
    void handlesNullVariablesMap() {
        String template = "No variables needed here";

        String result = renderingService.render(template, null);

        assertEquals("No variables needed here", result);
    }
}
