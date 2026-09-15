package com.outreach.platform.notification.service;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

/** Renders Thymeleaf templates from database-stored strings. */
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

    /** Renders a Thymeleaf template string with the given variables. */
    public String render(String templateContent, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        return stringTemplateEngine.process(templateContent, context);
    }
}
