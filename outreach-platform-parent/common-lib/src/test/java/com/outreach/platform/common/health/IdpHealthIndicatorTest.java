package com.outreach.platform.common.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdpHealthIndicator")
class IdpHealthIndicatorTest {

    private static final String JWKS_URI = "http://localhost:8090/oauth2/jwks";
    private static final String JWKS_PROPERTY = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";

    @Mock
    private Environment environment;

    @Mock
    private ObjectProvider<RestClient.Builder> restClientBuilderProvider;

    @Test
    @DisplayName("returns UP when JWKS endpoint returns valid response with keys")
    void returnsUpWhenJwksEndpointValid() {
        String validJwksResponse = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"test-key\"}]}";

        IdpHealthIndicator indicator = createIndicator(JWKS_URI, mockRestClientReturning(validJwksResponse));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("jwksUri", JWKS_URI);
    }

    @Test
    @DisplayName("returns DOWN when JWKS response does not contain keys field")
    void returnsDownWhenJwksResponseInvalid() {
        String invalidResponse = "{\"error\":\"not_found\"}";

        IdpHealthIndicator indicator = createIndicator(JWKS_URI, mockRestClientReturning(invalidResponse));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("jwksUri", JWKS_URI);
        assertThat(health.getDetails().get("error")).asString()
                .contains("missing 'keys' field");
    }

    @Test
    @DisplayName("returns DOWN when JWKS endpoint request throws exception")
    void returnsDownWhenJwksRequestFails() {
        IdpHealthIndicator indicator = createIndicator(JWKS_URI,
                mockRestClientThrowing(new RuntimeException("Connection refused")));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("jwksUri", JWKS_URI);
        assertThat(health.getDetails().get("error")).asString().contains("Connection refused");
    }

    @Test
    @DisplayName("returns UNKNOWN when JWKS URI is blank")
    void returnsUnknownWhenJwksUriBlank() {
        IdpHealthIndicator indicator = createIndicator("  ", mockRestClientReturning("unused"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails().get("reason")).asString().contains("not configured");
    }

    @Test
    @DisplayName("returns UNKNOWN when JWKS URI is null")
    void returnsUnknownWhenJwksUriNull() {
        IdpHealthIndicator indicator = createIndicator(null, mockRestClientReturning("unused"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails().get("reason")).asString().contains("not configured");
    }

    private IdpHealthIndicator createIndicator(String jwksUri, RestClient restClient) {
        when(environment.getProperty(JWKS_PROPERTY)).thenReturn(jwksUri);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.build()).thenReturn(restClient);
        when(restClientBuilderProvider.getIfAvailable(any())).thenReturn(builder);

        return new IdpHealthIndicator(environment, restClientBuilderProvider);
    }

    @SuppressWarnings("unchecked")
    private RestClient mockRestClientReturning(String body) {
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        RestClient.RequestHeadersUriSpec requestSpec = mock(RestClient.RequestHeadersUriSpec.class, RETURNS_DEEP_STUBS);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        lenient().when(restClient.get()).thenReturn(requestSpec);
        lenient().when(requestSpec.uri(any(String.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.body(String.class)).thenReturn(body);

        return restClient;
    }

    @SuppressWarnings("unchecked")
    private RestClient mockRestClientThrowing(RuntimeException ex) {
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        RestClient.RequestHeadersUriSpec requestSpec = mock(RestClient.RequestHeadersUriSpec.class, RETURNS_DEEP_STUBS);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(ex);

        return restClient;
    }
}
