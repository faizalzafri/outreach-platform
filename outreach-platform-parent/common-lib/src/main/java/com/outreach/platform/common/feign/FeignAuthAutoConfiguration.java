package com.outreach.platform.common.feign;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.common.tenant.TenantContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto-configuration for service-to-service Feign calls.
 *
 * <p>Acquires an OAuth2 client-credentials token for the shared {@code outreach-services} client
 * (registered in auth-service, see {@code AuthorizationServerConfig.buildServicesClient}) and
 * attaches it, plus the current {@code X-Tenant-ID}, to every outbound Feign request. Spring Cloud
 * OpenFeign applies any {@code RequestInterceptor} bean in the application context to all Feign
 * clients automatically, so nothing else needs to reference this class.
 *
 * <p>Inert wherever a consuming service hasn't configured
 * {@code spring.security.oauth2.client.registration.outreach-services} (every test profile, and
 * any service with no Feign clients) — {@link #authorizedClientManager} backs off via
 * {@code @ConditionalOnBean(ClientRegistrationRepository.class)}, which Spring Boot only
 * registers when that property is present.
 */
@AutoConfiguration
@AutoConfigureAfter(OAuth2ClientAutoConfiguration.class)
public class FeignAuthAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FeignAuthAutoConfiguration.class);

    /**
     * Non-request-bound authorized-client manager (unlike the default, HTTP-session-bound
     * manager) — required because the caller here is often a background/{@code @Async} thread
     * with no active {@code HttpServletRequest}, e.g. ingestion-service's import processor.
     */
    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        log.info("Registered OAuth2AuthorizedClientManager for service-to-service Feign auth");
        return manager;
    }

    // Nested config loaded only when Feign is on the classpath. Must live in its own
    // class-level-guarded nested @Configuration rather than a @Bean method on the outer class:
    // the RequestInterceptor return type requires feign.RequestInterceptor to be linkable
    // regardless of the @ConditionalOnBean outcome below — same reasoning as
    // TenantAutoConfiguration's AMQP/Redis nested configs.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    static class FeignRequestInterceptorConfiguration {

        // ObjectProvider instead of a hard constructor dependency + @ConditionalOnBean: this bean
        // must exist unconditionally whenever Feign is on the classpath (so it's always applied to
        // every Feign client, per Spring Cloud OpenFeign's global-RequestInterceptor mechanism),
        // and simply becomes a no-op when no OAuth2AuthorizedClientManager was registered (test
        // profiles, services with no client-credentials registration configured) — resolved lazily
        // per-request rather than at bean-creation time, sidestepping auto-configuration ordering
        // between this nested class and #authorizedClientManager entirely.
        @Bean
        public feign.RequestInterceptor serviceAuthRequestInterceptor(
                ObjectProvider<OAuth2AuthorizedClientManager> authorizedClientManagerProvider) {
            return template -> {
                OAuth2AuthorizedClientManager authorizedClientManager = authorizedClientManagerProvider.getIfAvailable();
                if (authorizedClientManager == null) {
                    return;
                }
                var authorizeRequest = org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
                        .withClientRegistrationId("outreach-services")
                        .principal("internal-service")
                        .build();
                var authorizedClient = authorizedClientManager.authorize(authorizeRequest);
                if (authorizedClient != null) {
                    template.header("Authorization", "Bearer " + authorizedClient.getAccessToken().getTokenValue());
                }
                if (TenantContext.isPresent()) {
                    template.header(TenantConstants.X_TENANT_ID_HEADER, TenantContext.getCurrentTenantId().toString());
                }
            };
        }
    }
}
