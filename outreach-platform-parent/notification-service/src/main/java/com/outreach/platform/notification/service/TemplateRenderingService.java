package com.outreach.platform.notification.service;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

/**
 * Renders notification templates from database-stored Thymeleaf strings.
 * Uses a dedicated StringTemplateResolver so templates are resolved from
 * in-memory strings rather than the classpath.
 */
@Service
public class TemplateRenderingService {

    private final TemplateEngine stringTemplateEngine;

    @Inject
    public TemplateRenderingService() {
        this.stringTemplateEngine = new TemplateEngine();
    }

    @PostConstruct
    void configureEngine() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        stringTemplateEngine.setTemplateResolver(resolver);
    }

    /**
     * Renders a Thymeleaf template string with the given variables.
     *
     * @param templateContent the Thymeleaf HTML template content (e.g. "Hello [[${name}]]")
     * @param variables       map of variable names to values injected into the template
     * @return the rendered HTML string
     */
    public String render(String templateContent, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        return stringTemplateEngine.process(templateContent, context);
    }
}
