package com.outreach.platform.common.config;

import com.outreach.platform.common.error.GlobalExceptionHandler;
import com.outreach.platform.common.filter.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * The shared error shape and correlation ids for every servlet service. They used to be picked up
 * only by component scanning of {@code com.outreach.platform}, so services that scan just their own
 * package (feedback-, ai- and auth-service) answered errors in Spring's default format, without
 * correlation ids. Services that do scan already have these beans, so nothing is registered twice.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletDefaultsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}
