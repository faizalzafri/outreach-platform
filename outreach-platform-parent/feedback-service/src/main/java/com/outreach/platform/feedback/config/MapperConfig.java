package com.outreach.platform.feedback.config;

import com.outreach.platform.feedback.mapper.FeedbackMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit mapper bean registration to avoid component scan conflicts
 * with multiple Spring Data modules on the classpath.
 */
@Configuration
public class MapperConfig {

    @Bean
    public FeedbackMapper feedbackMapper() {
        return Mappers.getMapper(FeedbackMapper.class);
    }
}
