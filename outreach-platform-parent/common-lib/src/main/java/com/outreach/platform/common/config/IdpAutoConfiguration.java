package com.outreach.platform.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Registers IdpProperties for any service that includes common-lib on the classpath.
 */
@Configuration
@EnableConfigurationProperties(IdpProperties.class)
public class IdpAutoConfiguration {

    /**
     * Shared JWT-to-authorities converter, available for any service's own SecurityConfig to wire
     * into its resource-server JWT configuration via
     * {@code oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))}. Not applied
     * automatically — Spring Security has no global extension point for this, so each service's
     * SecurityFilterChain bean must reference it explicitly.
     */
    @Bean
    @ConditionalOnClass(JwtAuthenticationToken.class)
    @ConditionalOnMissingBean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new RealmRoleJwtAuthenticationConverter();
    }
}
