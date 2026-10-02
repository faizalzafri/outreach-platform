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

    /**
     * {{name}} placeholders, which templates are written with, become Thymeleaf's escaped inline
     * expression [[${name}]]: Thymeleaf alone would leave {{name}} in the email as literal text.
     */
    private static final java.util.regex.Pattern PLACEHOLDER =
            java.util.regex.Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*}}");

    /** Renders a template string ({{name}} placeholders or Thymeleaf syntax) with the given variables. */
    public String render(String templateContent, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        String thymeleaf = PLACEHOLDER.matcher(templateContent).replaceAll("[[\\${$1}]]");
        return stringTemplateEngine.process(thymeleaf, context);
    }
}
