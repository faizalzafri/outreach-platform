package com.outreach.platform.common.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.core.convert.converter.Converter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extracts granted authorities from the JWT's {@code realm_access.roles} claim (already
 * {@code ROLE_}-prefixed by the issuing auth-service, mirroring Keycloak's claim shape) instead
 * of Spring Security's default behavior of deriving {@code SCOPE_*} authorities from the
 * {@code scope} claim alone.
 *
 * <p>Diagnosed via real browser + real JWT testing: every resource server's {@code SecurityConfig}
 * called {@code .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))} with no custom converter,
 * so the granted authorities for every authenticated request were only ever {@code SCOPE_openid},
 * {@code SCOPE_profile}, {@code SCOPE_email} — {@code realm_access.roles} was silently ignored.
 * Any {@code @PreAuthorize("hasRole(...)")}/{@code hasAnyRole(...)} check anywhere in the platform
 * could never pass for any user, regardless of their actual role — confirmed by an ADMIN user
 * getting a 403 from an endpoint whose only requirement was {@code hasAnyRole('TENANT_ADMIN',
 * 'ADMIN', 'PLATFORM_ADMIN')}.</p>
 */
public class RealmRoleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, extractAuthorities(jwt));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .map(String::valueOf)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
