package com.outreach.platform.notification.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

/** Renders Thymeleaf templates from database-stored strings. */
@Service
public class TemplateRenderingService {

    private final TemplateEngine stringTemplateEngine = new TemplateEngine();

    public TemplateRenderingService() {
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
