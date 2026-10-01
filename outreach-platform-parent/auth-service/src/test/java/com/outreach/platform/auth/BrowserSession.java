package com.outreach.platform.auth;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A minimal browser for integration tests: keeps cookies across requests (so the server-side
 * session and saved request survive), never follows redirects (tests assert each hop), and
 * submits forms with the page's CSRF token.
 */
final class BrowserSession {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    static final String CALLBACK = "http://localhost:5173/callback";

    private final String baseUrl;
    private String verifier;
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    BrowserSession(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    HttpResponse<String> get(String pathOrUrl) {
        return send(HttpRequest.newBuilder(uri(pathOrUrl)).GET().build());
    }

    /** GETs the form page, then POSTs the fields plus its CSRF token. */
    HttpResponse<String> submitForm(String formPath, String actionPath, Map<String, String> fields) {
        Matcher csrf = CSRF.matcher(get(formPath).body());
        Map<String, String> body = new LinkedHashMap<>(fields);
        if (csrf.find()) {
            body.put("_csrf", csrf.group(1));
        }
        return post(actionPath, body);
    }

    HttpResponse<String> post(String path, Map<String, String> fields) {
        String encoded = fields.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encoded))
                .build());
    }

    HttpResponse<String> login(String username, String password) {
        return submitForm("/login", "/login", Map.of("username", username, "password", password));
    }

    /** Starts the dashboard's PKCE authorization; the server answers with a redirect to /login. */
    HttpResponse<String> startAuthorization() {
        verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes());
        String challenge;
        try {
            challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        return get("/oauth2/authorize?response_type=code&client_id=outreach-dashboard"
                + "&redirect_uri=" + enc(CALLBACK) + "&scope=" + enc("openid profile email")
                + "&code_challenge=" + challenge + "&code_challenge_method=S256&state=test");
    }

    /**
     * Continues an authorization started with {@link #startAuthorization} after the user has
     * signed in (the response that completed sign-in), and exchanges the code for an access token.
     */
    String finishAuthorization(HttpResponse<String> signedIn) {
        String callback = followToExternal(signedIn);
        if (callback == null || !callback.contains("code=")) {
            throw new AssertionError("Expected a redirect to the callback with a code, got: " + location(signedIn));
        }
        String code = URI.create(callback).getQuery().replaceAll(".*code=([^&]+).*", "$1");
        HttpResponse<String> token = post("/oauth2/token", Map.of(
                "grant_type", "authorization_code",
                "client_id", "outreach-dashboard",
                "code", java.net.URLDecoder.decode(code, StandardCharsets.UTF_8),
                "redirect_uri", CALLBACK,
                "code_verifier", verifier));
        Matcher accessToken = Pattern.compile("\"access_token\":\"([^\"]+)\"").matcher(token.body());
        if (!accessToken.find()) {
            throw new AssertionError("Token exchange failed: " + token.statusCode() + " " + token.body());
        }
        return accessToken.group(1);
    }

    /** Full password sign-in through the dashboard client; returns the access token. */
    String signIn(String username, String password) {
        startAuthorization();
        return finishAuthorization(login(username, password));
    }

    /**
     * Sign-in that copes with a one-time-code step: if the server asks for a code, it is looked
     * up with {@code codeFor} (given the username) and entered.
     */
    String signInWithAnyCode(String username, String password, java.util.function.Function<String, String> codeFor) {
        startAuthorization();
        HttpResponse<String> afterPassword = login(username, password);
        if (location(afterPassword).endsWith("/login/otp")) {
            afterPassword = submitForm("/login/otp", "/login/otp", Map.of("code", codeFor.apply(username)));
        }
        return finishAuthorization(afterPassword);
    }

    /** The decoded JSON payload of a JWT. */
    static String claims(String jwt) {
        return new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    /** Follows a redirect chain while it stays on the auth server; returns the first off-server hop's URL. */
    String followToExternal(HttpResponse<String> response) {
        HttpResponse<String> current = response;
        for (int hops = 0; hops < 10; hops++) {
            String location = current.headers().firstValue("Location").orElse(null);
            if (location == null) {
                return null;
            }
            if (!location.startsWith(baseUrl) && !location.startsWith("/")) {
                return location;
            }
            current = get(location);
        }
        throw new IllegalStateException("Too many redirects");
    }

    /** A JSON API call with a bearer token (no cookies needed, but harmless). */
    HttpResponse<String> api(String method, String path, String accessToken, String jsonBody) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json");
        if (jsonBody != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return send(request.build());
    }

    static String location(HttpResponse<String> response) {
        return response.headers().firstValue("Location").orElse("");
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private URI uri(String pathOrUrl) {
        return URI.create(pathOrUrl.startsWith("http") ? pathOrUrl : baseUrl + pathOrUrl);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
