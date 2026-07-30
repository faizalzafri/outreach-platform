package com.outreach.platform.feedback.config;

import com.outreach.platform.feedback.mapper.FeedbackMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MapStruct mapper beans to avoid component scan conflicts. */
@Configuration
public class MapperConfig {

    @Bean
    public FeedbackMapper feedbackMapper() {
        return Mappers.getMapper(FeedbackMapper.class);
    }
}
