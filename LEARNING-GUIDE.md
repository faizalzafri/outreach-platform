# Learning Guide — Backend Modernization

This guide documents the framework features, design patterns, and engineering concepts used throughout the modernization project. Use it as a reference to deepen your understanding of each component.

---

## Task 1: Project Structure, Parent POM, and Shared Modules

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Multi-Module Maven Project** | A parent POM with `<modules>` declarations manages a set of child modules sharing dependency versions, plugin config, and build lifecycle. | Ensures consistent versions across all microservices. One version bump in the parent propagates everywhere. |
| **Spring Boot Starter Parent** | `spring-boot-starter-parent` provides default plugin configurations, dependency management, and sensible defaults. | Removes boilerplate — you get sane compiler settings, test runners, and a curated set of dependency versions. |
| **BOM (Bill of Materials) Import** | `<dependencyManagement>` with `<scope>import</scope>` imports version constraints from external BOMs without inheriting them as parent POMs. | Allows combining multiple managed dependency sets without Maven's single-inheritance limitation. |
| **`@RestControllerAdvice`** | A specialization of `@ControllerAdvice` that applies `@ResponseBody` to all `@ExceptionHandler` methods. Intercepts exceptions globally. | Centralizes error handling. Every service gets consistent error responses without duplicating try-catch logic. |
| **Bean Validation (JSR 380)** | Declarative validation via annotations (`@NotNull`, `@NotBlank`, `@Email`, `@Size`) on DTOs, triggered by `@Valid`. | Separates validation rules from business logic. Invalid requests never reach the service layer. |
| **JPA `AttributeConverter`** | Transforms entity field values before writing to DB and after reading. | Enables transparent encryption at the persistence layer. Application code works with plaintext; the database stores ciphertext. |
| **JPA Auditing** | `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` auto-populate audit fields on persist/update. | Provides a reliable audit trail with no developer effort per entity. |
| **`@MappedSuperclass`** | Base class whose fields are inherited by JPA entities but which is not itself an entity (no table). | Reusable base for common fields (id, version, audit columns) without table-per-hierarchy complications. |
| **Optimistic Locking (`@Version`)** | A version field incremented on each update. Concurrent conflicting updates throw `OptimisticLockingFailureException`. | Prevents lost updates without pessimistic locks. Essential for concurrent web applications. |
| **Jakarta Servlet `Filter`** | Intercepts every HTTP request/response before reaching a controller. | Perfect for cross-cutting concerns: correlation IDs, authentication, logging. |
| **SLF4J MDC** | Thread-local key-value store included in every log statement from that thread. | Enables request-scoped context (correlation ID, trace ID) in logs without passing parameters through every method. |
| **Logback `ClassicConverter`** | Custom conversion specifier transforming log messages before output. | PII/secret masking applied transparently at the logging layer — no changes to application code. |
| **LogstashEncoder + `JsonGeneratorDecorator`** | Structured JSON log output with a decorator intercepting all string writes for masking. | Machine-parseable logs for aggregation. Masking applies to ALL JSON fields, not just message. |
| **Java Records** | Immutable data carriers with compiler-generated `equals()`, `hashCode()`, `toString()`. | Ideal for DTOs and value objects. Less boilerplate, communicates immutability intent. |
| **Spring Security `SecurityContextHolder`** | Static access to the current thread's authentication state. | `AuditorAware` reads the authenticated principal to populate `@CreatedBy`/`@LastModifiedBy`. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Template Method (Filter Chain)** | `CorrelationIdFilter` calls `chain.doFilter()` — adds behavior before/after the template. |
| **Decorator** | `MaskingJsonGeneratorDecorator` wraps a `JsonGenerator`, adding masking without modifying the original encoder. |
| **Chain of Responsibility** | Logback converters apply sequential transformations. `MaskingCompositeConverter` composes them. |
| **Strategy** | `AesEncryptionConverter` encapsulates the encryption algorithm. Swappable without touching entities. |
| **Record as Value Object** | `ErrorResponse` is immutable. No setters, no inheritance, no identity — just data. |
| **Mapped Superclass** | `BaseEntity` shares audit/version fields across all entities without creating a single table. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@RestControllerAdvice` | Global exception handler returning JSON |
| `@ExceptionHandler(Type.class)` | Maps a specific exception type to a handler method |
| `@Converter` (JPA) | Registers an `AttributeConverter` |
| `@MappedSuperclass` | Base class for JPA field inheritance |
| `@Version` | Optimistic locking version counter |
| `@Order(Ordered.HIGHEST_PRECEDENCE)` | Ensures filter runs first in the chain |
| `MDC.put()` / `MDC.remove()` | Thread-local context for structured logging |
| `GCMParameterSpec` | Configures AES-GCM authenticated encryption |
| `SecureRandom` | Cryptographically strong random for IVs |

### Concepts to Study Further

1. **AES-GCM Authenticated Encryption** — Why GCM provides both confidentiality and integrity, why IV must never repeat, how the 128-bit auth tag prevents tampering.
2. **Servlet Filter Chain vs. Spring Security Filter Chain** — These are distinct but related. Learn how `DelegatingFilterProxy` → `FilterChainProxy` works.
3. **MDC Propagation in Async/Virtual Threads** — MDC is thread-local. Study `TaskDecorator`, Micrometer's context propagation, and `ContextSnapshot`.

---

## Task 3.1: Implement Config Server

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Spring Cloud Config Server** | Serves externalized configuration to all microservices over HTTP (`/{application}/{profile}`). | Decouples configuration from code artifacts. Same JAR runs in dev, staging, prod. |
| **`@EnableConfigServer`** | Activates Config Server REST endpoints, property resolution, and encryption services. | One annotation boots an entire infrastructure service. |
| **Native Filesystem Backend** | `spring.cloud.config.server.native.searchLocations` serves config from a local directory. | Fast iteration in dev — edit YAML, refresh, done. |
| **Git-Backed Backend** | `spring.cloud.config.server.git.uri` serves config from a git repo with branch/tag support. | Production-grade: version-controlled, auditable, PR-reviewable config changes. |
| **Spring Cloud Config Encryption** | `encrypt.key` enables symmetric encryption. `{cipher}` prefixed properties are decrypted server-side. | Secrets in config files without plaintext exposure. Key itself comes from env var. |
| **Spring Boot Profiles** | `application-{profile}.yml` files define environment-specific overrides. | One codebase, multiple environments. Profile activation is runtime, not build-time. |
| **`/actuator/refresh`** | Causes `@RefreshScope` beans to reload configuration without restart. | Runtime configuration changes (feature toggles, rate limits) without downtime. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Externalized Configuration** | All environment-specific values live outside the deployable artifact. |
| **Profile-based Strategy** | Different profiles select different strategies (local DB vs. encrypted prod credentials). |
| **Encryption at Rest** | `{cipher}` values stored encrypted; decrypted at serve-time. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@EnableConfigServer` | Activates Spring Cloud Config Server |
| `spring.cloud.config.server.native.searchLocations` | Filesystem config backend |
| `spring.cloud.config.server.git.uri` | Git config backend |
| `encrypt.key` | Symmetric key for `{cipher}` decryption |
| `/encrypt` and `/decrypt` | REST endpoints for value encryption |

### Concepts to Study Further

1. **Config Server Security Model** — Client auth (HTTP Basic, mTLS, token relay), encryption key rotation, asymmetric RSA keys vs. symmetric.
2. **Spring Cloud Bus** — Broadcasting `/actuator/refresh` to all instances via RabbitMQ/Kafka.
3. **Configuration Precedence** — The 17+ property sources Spring Boot checks and `@ConfigurationProperties` type-safe binding.

---

## Task 3.2: Implement Discovery Service (Eureka Server)

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Spring Cloud Netflix Eureka Server** | Service registry where microservices register and discover each other at runtime. | Eliminates hardcoded URLs. Enables horizontal scaling and zero-downtime deployments. |
| **`@EnableEurekaServer`** | Activates Eureka Server exposing registration, heartbeat, and query endpoints. | One annotation starts an entire service registry. |
| **Self-Preservation Mode** | When heartbeat rate drops below threshold, stops evicting instances (assumes network partition). | Prevents cascading failures during network issues. |
| **Eviction Timer** | Controls how frequently Eureka checks for expired leases. | Trade-off: fast eviction (dev) = quick feedback; slow eviction (prod) = fewer false positives. |
| **Lease Renewal & Expiration** | Clients heartbeat every N seconds; if no heartbeat within M seconds, instance is evicted. | Defines the failure detection window. Shorter = faster detection, more false positives. |
| **Spring Security Filter Chain** | Composable security via `HttpSecurity` builder pattern. | Separates security policy from business logic. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Service Registry** | Runtime registry — services register on boot, query at call-time. |
| **Heartbeat / Lease** | Time-bounded "I'm alive" contract for distributed failure detection. |
| **Self-Preservation (Circuit Breaker for registries)** | Failure rate exceeds threshold → stop evictions to prevent cascading failures. |
| **Standalone vs. Peer-Aware (Strategy via Profiles)** | Same binary, different behavior based on configuration. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@EnableEurekaServer` | Activates Eureka Server |
| `eureka.client.register-with-eureka` | Whether to register with another Eureka instance |
| `eureka.server.enable-self-preservation` | Protection against mass eviction |
| `eureka.server.eviction-interval-timer-in-ms` | Lease check frequency |
| `HttpSecurity.authorizeHttpRequests()` | URL-based authorization rules |

### Concepts to Study Further

1. **CAP Theorem and Eureka's AP Choice** — Eureka is AP (Available + Partition-tolerant, sacrificing Consistency). Compare with Consul (CP) and ZooKeeper (CP).
2. **Service Mesh vs. Client-Side Discovery** — Eureka is client-side discovery. Compare with server-side (load balancer) and service mesh (Envoy/Istio sidecar).
3. **Eureka Replication Protocol** — Multi-instance eventual consistency, split-brain handling, and cluster bootstrap timing.

---

## Task 3.3: Implement API Gateway with Spring Cloud Gateway

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Spring Cloud Gateway** | Reactive API gateway built on Project Reactor and Netty. Routes requests to downstream services using predicates and filters. | Replaces deprecated Netflix Zuul with a non-blocking, reactive gateway that integrates natively with Spring Cloud service discovery. |
| **`spring-cloud-starter-gateway`** | Starter that auto-configures route locators, filter factories, and the Netty-based server. | One dependency brings the entire reactive routing infrastructure — no Servlet container needed. |
| **Route Predicates** | `Path=`, `Host=`, `Method=` — declarative conditions determining which route matches a request. | Separates routing logic from code. YAML-driven route changes don't require recompilation. |
| **`StripPrefix` Filter** | Removes N path segments before forwarding. `StripPrefix=1` turns `/api/feedback/scores` into `/feedback/scores`. | Allows a clean public URL namespace (`/api/...`) while downstream services use their own path roots. |
| **`RequestRateLimiter` Filter** | Built-in GatewayFilter backed by Redis that uses the Token Bucket algorithm for rate limiting. | Production-ready rate limiting with configurable replenish rate, burst capacity, and key resolver — no custom code required for the authenticated path. |
| **`CircuitBreaker` Default Filter** | Default filter applying Resilience4j circuit breaker to every route with a fallback URI. | If any downstream service is slow or failing, requests fail fast to the fallback rather than queueing indefinitely. |
| **Spring Security OAuth2 Resource Server (Reactive)** | Validates JWT access tokens using JWKS endpoint. Operates in WebFlux's non-blocking model. | Every request to a protected route is authenticated via public key verification — no shared secret, no call to the IdP per request. |
| **`EnableWebFluxSecurity`** | Activates reactive Spring Security filter chain (as opposed to Servlet-based `@EnableWebSecurity`). | Gateway is Netty-based, not Tomcat. Security must use `ServerHttpSecurity` and `SecurityWebFilterChain`. |
| **`ReactiveStringRedisTemplate`** | Reactive Redis operations (non-blocking I/O via Lettuce driver). | Rate limiting in a reactive pipeline — Redis calls don't block Netty event loop threads. |
| **`GlobalFilter` + `Ordered`** | Filters that apply to ALL routes (not per-route). Order determines execution sequence. | Cross-cutting concerns (correlation ID generation, adaptive rate limiting) applied once, universally. |
| **`@ConfigurationProperties`** | Type-safe binding of YAML properties to a Java bean. | CORS origins, rate limits, and other gateway settings are configurable without code changes. |
| **Resilience4j `TimeLimiter`** | Caps downstream call duration. Default 5s means the gateway always responds within 5 seconds. | Prevents gateway threads from being held indefinitely by slow backends. Meets the "503 within 5 seconds" requirement. |
| **Spring Boot Actuator** | Exposes `/actuator/health`, `/actuator/metrics`, `/actuator/circuitbreakers` for operational visibility. | Liveness/readiness probes for container orchestration; circuit breaker state inspection for debugging. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Chain of Responsibility** | Gateway filters form an ordered chain. Each filter (correlation ID → rate limit → security → circuit breaker → routing) decides whether to continue or short-circuit. |
| **Strategy (KeyResolver)** | `authenticatedKeyResolver` and `ipKeyResolver` are interchangeable strategies for determining the rate-limit key. The gateway selects the strategy based on authentication state. |
| **Circuit Breaker** | Resilience4j wraps downstream calls. After 50% failures over 10 calls, the breaker opens and routes directly to the fallback — protecting the gateway from cascading failure. |
| **Fallback / Degraded Response** | `FallbackController` provides a structured 503 response for all HTTP methods when the circuit is open. Clients get a predictable error rather than a timeout. |
| **Token Bucket (Rate Limiting)** | Redis-backed token bucket (built-in `RequestRateLimiter`) refills at 100 tokens/min for authenticated users. The `AdaptiveRateLimitFilter` implements a fixed-window counter (20/min) for unauthenticated traffic. |
| **Proxy / Reverse Proxy** | The gateway itself is a reverse proxy — clients speak to one endpoint, unaware of backend topology. |
| **Decorator** | `ServerWebExchange.mutate()` wraps the original exchange with additional headers (correlation ID) without modifying the original immutable object. |

### Key Annotations & APIs

| Annotation / API | Purpose | Docs |
|-----------------|---------|------|
| `@EnableWebFluxSecurity` | Activates reactive security filter chain | [Spring Security Reactive](https://docs.spring.io/spring-security/reference/reactive/configuration/webflux.html) |
| `ServerHttpSecurity` | Builder for reactive security rules (authorize, OAuth2, CSRF) | [ServerHttpSecurity API](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/web/server/ServerHttpSecurity.html) |
| `GlobalFilter` | Interface for filters applied to all routes | [Spring Cloud Gateway Filters](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/global-filters.html) |
| `GatewayFilterChain` | Provides `filter(exchange)` to continue the chain | — |
| `ServerWebExchange.mutate()` | Creates a modified copy of the exchange (immutable pattern) | — |
| `ReactiveStringRedisTemplate.opsForValue().increment()` | Atomic Redis INCR for counter-based rate limiting | [Spring Data Redis Reactive](https://docs.spring.io/spring-data/redis/reference/redis/reactive.html) |
| `@ConfigurationProperties(prefix = "...")` | Binds YAML properties to a typed bean | [Type-safe Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties) |
| `CorsWebFilter` + `UrlBasedCorsConfigurationSource` | Reactive CORS handling applied before routing | [Spring WebFlux CORS](https://docs.spring.io/spring-framework/reference/web/webflux-cors.html) |
| `Ordered.HIGHEST_PRECEDENCE` | Controls filter ordering; lower value = earlier execution | — |

### Concepts to Study Further

1. **Reactive Programming and Project Reactor** — The gateway runs on Netty with `Mono<Void>` return types. Understanding backpressure, `flatMap` vs `switchIfEmpty`, and the reactive `Publisher` contract is essential for debugging gateway filters. Start with [Reactor Core reference](https://projectreactor.io/docs/core/release/reference/).
2. **Token Bucket vs. Fixed Window vs. Sliding Window Rate Limiting** — The built-in `RequestRateLimiter` uses token bucket (allows bursts). The custom `AdaptiveRateLimitFilter` uses a fixed-window counter (simpler but has boundary spike issues). Study the trade-offs and when to use sliding window (Redis sorted sets).
3. **JWKS Token Validation Flow** — When the gateway receives a JWT, it fetches the public key from the JWKS endpoint, caches it, and validates the signature locally. No round-trip to the IdP per request. Study JWK key rotation, kid (key ID) matching, and token introspection as an alternative.

---

## Task 3.4: Integration Tests for Config Server, Eureka, and Gateway

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** | Boots the full application context on a random available port, enabling HTTP-level integration testing. | Tests validate the real request-response flow — filters, security, routing — not just controller methods in isolation. |
| **`WebTestClient`** | Non-blocking, reactive test client for WebFlux applications. Provides fluent assertions on status, headers, and body. | The gateway is Netty-based (not Servlet). `TestRestTemplate` doesn't work here — `WebTestClient` speaks the reactive HTTP protocol. |
| **`@DynamicPropertySource`** | Static method that injects properties into the Spring `Environment` at context startup. Values can reference runtime state (e.g., WireMock port). | Solves the chicken-and-egg problem: WireMock starts on a dynamic port, but Spring needs the JWKS URL at context creation time. |
| **`@ActiveProfiles("test")`** | Activates the `test` profile, loading `application-test.yml` and triggering `@Profile("test")` beans. | Enables test-specific security configuration (permitting `/api/test/**`) without weakening production security. |
| **WireMock** | Embedded HTTP server that stubs external service responses. Configured programmatically with request matching and response templates. | Simulates both the JWKS endpoint (for JWT validation) and downstream backend services (for route forwarding). No real IdP or microservice needed. |
| **Nimbus JOSE+JWT** | Java library for creating, signing, and verifying JWTs and JWKS. Used to generate RSA key pairs and sign test tokens. | Allows the test to create valid/expired/invalid JWTs matching the exact JWKS that WireMock serves — full end-to-end JWT validation without Keycloak. |
| **`ReactiveStringRedisTemplate`** | Reactive Redis operations for clearing rate-limit keys between tests. Uses `keys()` + `delete()` in a reactive pipeline. | Tests must be isolated. Clearing Redis state in `@BeforeEach` ensures rate-limit counters don't leak between test methods. |
| **`@Nested` Test Classes** | JUnit 5 feature for grouping related tests into inner classes with shared `@DisplayName`. | Organizes tests by concern (JWT, Rate Limiting, Circuit Breaker). Each nested class can have its own setup/teardown. |
| **`@BeforeAll` / `@AfterAll`** | Class-level lifecycle methods for expensive setup (WireMock servers, RSA key generation). Run once per test class, not per method. | RSA key generation is expensive (~200ms). Generating once and reusing across all JWT tests keeps the suite fast. |
| **Profile-Conditional `@Bean`** | `@Profile("!test")` / `@Profile("test")` conditionally registers different `SecurityWebFilterChain` beans. | Test profile permits test-only routes (`/api/test/**`) while production profile enforces full authentication. Same code, different behavior per environment. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Test Double (Stub)** | WireMock acts as a stub for the JWKS endpoint and downstream services. It returns pre-configured responses without real logic. |
| **Test Fixture (Object Mother)** | `generateValidJwt()` and `generateExpiredJwt()` are factory methods producing JWT tokens with specific characteristics for each test scenario. |
| **Test Isolation** | `@BeforeEach clearRateLimitKeys()` ensures each rate-limit test starts from zero. Without this, test ordering would affect results. |
| **Decorator (ServerHttpResponseDecorator)** | The `CorrelationIdFilter` wraps the response object to inject headers just before body write — proper reactive gateway pattern for modifying responses without race conditions. |
| **Dynamic Port Binding** | Both WireMock and the Spring Boot server use random ports, then `@DynamicPropertySource` wires them together. Eliminates port conflicts in CI. |
| **Strategy (Profile-Based Security)** | Two `SecurityWebFilterChain` beans coexist; the active profile selects which one Spring instantiates. Same pattern used across the platform for IdP switching. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@DynamicPropertySource` | Injects runtime-resolved properties into Spring context |
| `WebTestClient.get().uri().header().exchange()` | Fluent reactive HTTP test API with status/body assertions |
| `WireMockServer.stubFor(get(...).willReturn(...))` | Declarative HTTP stub configuration |
| `RSAKeyGenerator(2048).generate()` | Generates RSA key pair for test JWT signing |
| `SignedJWT` + `RSASSASigner` | Creates a properly signed JWT for test authentication |
| `JWKSet.toString()` | Serializes JWKS to JSON for WireMock to serve |
| `ReactiveStringRedisTemplate.keys().flatMap(delete)` | Reactive pattern for bulk-deleting Redis keys |
| `@Nested` + `@DisplayName` | Organized test structure with descriptive names |
| `ServerHttpResponseDecorator` | Wraps gateway response to add headers before body commit |
| `expectStatus().isEqualTo(429)` | Asserts HTTP 429 Too Many Requests |

### Concepts to Study Further

1. **Reactive Testing Patterns** — `WebTestClient` vs `StepVerifier` vs `TestPublisher`. Understand how `block()` in `@BeforeEach` bridges reactive code into imperative test setup, and why it's safe in test context but dangerous in production reactive pipelines.
2. **WireMock Response Templating and Verification** — Beyond simple stubs, WireMock supports response templating (Handlebars), request matching with JSON body patterns, stateful scenarios, and verification (`verify(N, getRequestedFor(...))`) to assert how many times a stub was called.
3. **ServerHttpResponseDecorator in Reactive Gateway** — In a Netty-based reactive gateway, the response is committed (flushed to the network) the moment `writeWith()` is called. Any header modifications AFTER `chain.filter()` returns are too late. The Decorator pattern intercepts `writeWith()` to inject headers at the last safe moment. Study this pattern vs. the simpler Servlet `response.addHeader()` approach.

---

## Task 4.1: Configure Keycloak Realm and Deployment

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Keycloak Realm Export JSON** | A declarative JSON file that fully describes an identity realm — clients, roles, users, authentication flows, password policies, brute force settings. Keycloak imports this on startup to bootstrap the IdP. | Infrastructure-as-code for identity. The realm is version-controlled, reviewable, and reproducible across environments without manual UI clicks. |
| **OAuth 2.1 / OIDC 1.0 Protocol Support** | Keycloak implements the full OAuth 2.1 and OpenID Connect 1.0 specifications including Authorization Code + PKCE, Client Credentials, token refresh, and JWKS endpoint exposure. | Industry-standard authentication. Any OIDC-compliant client or resource server can integrate without vendor-specific logic. |
| **Public Client with PKCE (S256)** | `outreach-dashboard` is configured as `publicClient: true` with `pkce.code.challenge.method: S256`. No client secret is stored in the browser — the code verifier/challenge proves possession. | Eliminates the risk of client secret exposure in SPAs. PKCE prevents authorization code interception attacks even on public clients. |
| **Confidential Client with Service Accounts** | `outreach-services` is configured as `publicClient: false` with `serviceAccountsEnabled: true`. Uses Client Credentials flow for machine-to-machine communication. | Backend services authenticate without user interaction. The client secret is stored server-side (never in a browser), and service accounts can have their own role mappings. |
| **Realm Role Mapping** | Roles (`ROLE_ADMIN`, `ROLE_PMO`, `ROLE_POC`) are defined at the realm level and mapped into access tokens via protocol mappers. | Resource servers read roles from the JWT without calling back to Keycloak. One source of truth for authorization across all services. |
| **Password Policy DSL** | Keycloak's `passwordPolicy` field accepts a composable policy string: `length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1)`. | Declarative security policy. Enforced at the IdP level — no application code can bypass it. Changes propagate to all clients immediately. |
| **Brute Force Protection** | `bruteForceProtected: true`, `failureFactor: 5`, `waitIncrementSeconds: 1800` — locks accounts after 5 failures for 30 minutes. | Prevents credential stuffing and brute-force attacks at the identity layer. No application-level implementation needed. |
| **Required Actions (`UPDATE_PASSWORD`)** | The default admin user has `requiredActions: ["UPDATE_PASSWORD"]` and `temporary: true` credential. Keycloak forces a password change on first login. | Eliminates default-password vulnerabilities. Even provisioned accounts must set a unique password before accessing the system. |
| **Protocol Mappers (OIDC Role Mapper)** | `oidc-usermodel-realm-role-mapper` injects realm roles into `realm_access.roles` in the access token, ID token, and userinfo endpoint. | Resource servers extract roles directly from the JWT payload. No additional call to Keycloak required for authorization decisions. |
| **Event Logging (Login, Logout, Token)** | `eventsEnabled: true` with specific event types (`LOGIN`, `LOGIN_ERROR`, `LOGOUT`, `CODE_TO_TOKEN`, etc.) and admin events enabled with details. | Audit trail for all authentication events. Essential for security incident investigation and compliance reporting. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Infrastructure as Code** | The entire identity configuration lives in a JSON file under version control. No manual setup, no configuration drift between environments. |
| **Separation of Concerns (Identity vs. Application)** | Authentication and authorization policy is externalized to a dedicated identity provider. Application services only validate tokens — they don't manage users, passwords, or sessions. |
| **Principle of Least Privilege** | Three distinct roles with different access levels. The default admin gets `UPDATE_PASSWORD` required action — even privileged accounts start locked down. |
| **Defense in Depth** | Multiple layers: password complexity (prevents weak passwords), brute force protection (prevents automated attacks), PKCE (prevents code interception), temporary credentials (prevents default password abuse). |
| **Configuration-Driven Behavior** | All security policies (lockout threshold, password rules, token lifetimes) are data, not code. Tuning security posture requires editing JSON, not recompiling. |
| **Client Segmentation (Public vs. Confidential)** | Different client types for different trust levels. Browser clients are untrusted (public, PKCE). Server clients are trusted (confidential, client secret). Each gets the minimum capabilities needed. |

### Key Annotations & APIs

| Concept / Field | Purpose |
|-----------------|---------|
| `realm` | Top-level identity boundary. All users, clients, and roles exist within a realm. |
| `clients[].publicClient` | `true` = no client secret (browser SPA); `false` = confidential (backend service) |
| `clients[].attributes.pkce.code.challenge.method` | `S256` enables SHA-256 PKCE challenge. Required for public clients per OAuth 2.1. |
| `clients[].serviceAccountsEnabled` | Enables Client Credentials grant — machine-to-machine authentication without a user. |
| `passwordPolicy` | Composable string enforcing complexity rules at the Keycloak level. |
| `bruteForceProtected` + `failureFactor` + `waitIncrementSeconds` | Lockout configuration: N failures → M seconds lock. |
| `users[].requiredActions` | Actions the user must complete on next login (e.g., `UPDATE_PASSWORD`). |
| `users[].credentials[].temporary` | `true` means the password expires after first use — forces a reset. |
| `clientScopes[].protocolMappers` | Protocol mappers transform user attributes into JWT claims. |
| `defaultSignatureAlgorithm: RS256` | RSA-SHA256 for JWT signing. Resource servers verify via the JWKS public key endpoint. |
| `accessTokenLifespan: 300` | Access tokens expire in 5 minutes. Short-lived to limit damage from token theft. |
| `ssoSessionIdleTimeout: 1800` | SSO session expires after 30 minutes of inactivity. |

### Concepts to Study Further

1. **OAuth 2.1 vs OAuth 2.0 — What Changed** — OAuth 2.1 deprecates Implicit flow, requires PKCE for all public clients, and mandates refresh token rotation. Study [RFC 9126 (PAR)](https://datatracker.ietf.org/doc/rfc9126/), [RFC 7636 (PKCE)](https://datatracker.ietf.org/doc/rfc7636/), and the [OAuth 2.1 draft](https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/) to understand the security motivations behind each change.
2. **JWKS Key Rotation and `kid` Matching** — Keycloak rotates signing keys periodically. Resource servers cache the JWKS and match the `kid` (key ID) in the JWT header to find the correct public key. Study what happens during rotation (both old and new keys are served), grace periods, and how `NimbusJwtDecoder` handles cache refresh on unknown `kid`.
3. **Keycloak Realm vs. Client Scopes vs. Protocol Mappers** — The claim transformation pipeline: realm roles → client scope inclusion → protocol mapper → JWT claim. Understand how `fullScopeAllowed` bypasses scope restrictions, why production deployments should use fine-grained scope assignments, and how `audience` mappers restrict token usage to specific services.

---

## Task 4.2: Implement Spring Authorization Server Alternative

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Spring Authorization Server** | A framework-level implementation of OAuth 2.1 and OpenID Connect 1.0 that runs as an embedded Spring Boot application. Exposes standard endpoints: `/oauth2/authorize`, `/oauth2/token`, `/oauth2/revoke`, `/oauth2/jwks`, `/.well-known/openid-configuration`. | Provides a lightweight, self-contained IdP alternative to Keycloak. No external infrastructure dependency — the auth server is just another microservice in the platform. |
| **`JdbcRegisteredClientRepository`** | Spring's built-in JDBC-backed implementation of `RegisteredClientRepository`. Stores OAuth2 client registrations in a PostgreSQL table (`oauth2_registered_client`). | Persistent client storage survives restarts. Clients are data, not code — new clients can be registered at runtime without redeployment. |
| **`JdbcOAuth2AuthorizationService`** | JDBC-backed storage for active authorizations (access tokens, refresh tokens, authorization codes). Required for token revocation per RFC 7009. | The revocation endpoint (`/oauth2/revoke`) looks up the token in this table and invalidates it. Without persistent storage, revocation is impossible — in-memory tokens are lost on restart. |
| **`JdbcOAuth2AuthorizationConsentService`** | Persists user consent decisions (which scopes a user granted to which client). | Users don't need to re-consent after server restart. Audit trail of what permissions were granted. |
| **`OAuth2TokenCustomizer<JwtEncodingContext>`** | Hook invoked just before a JWT is signed. Allows adding custom claims to access tokens. | Maps Spring Security authorities to `realm_access.roles` in the JWT — matching Keycloak's token format so resource servers work identically with either provider. |
| **`AuthorizationServerSettings`** | Configuration bean defining the issuer URI and all OAuth2 endpoint paths. | The issuer URI appears in the `iss` claim of every JWT. Resource servers validate this claim — it must match exactly. |
| **`ClientSettings.requireProofKey(true)`** | Forces PKCE on the Authorization Code flow for the dashboard client. | Matches Keycloak's PKCE configuration. Public clients without PKCE are vulnerable to authorization code interception attacks. |
| **`TokenSettings`** | Configures access token TTL, refresh token TTL, and token reuse policy per client. | Different clients need different lifetimes. Service-to-service tokens can be shorter; user-facing refresh tokens need longer duration for UX. |
| **`@ConfigurationProperties` with Java Records** | Type-safe, immutable configuration binding using Java 21 records. Validated with Bean Validation annotations (`@NotBlank`, `@Min`). | Configuration is a compile-time contract. Typos in property names cause startup failures, not silent null values. Records enforce immutability — config can't be mutated after binding. |
| **`@Order` on `SecurityFilterChain`** | Multiple filter chains coexist; `@Order(1)` processes authorization server requests first, `@Order(2)` handles form login. | Spring Security evaluates chains in order. The authorization server endpoints (JWKS, token, revoke) have stricter rules and must be matched before the general-purpose form login chain. |
| **`DaoAuthenticationProvider` (extended)** | The standard provider that loads `UserDetails` and verifies password hashes. Extended here with lockout-aware behavior. | Extending rather than replacing means all standard features (BCrypt verification, credential expiration) continue to work. The lockout check is an additional pre-condition. |
| **Spring Security Event System** | `AuthenticationFailureBadCredentialsEvent` and `AuthenticationSuccessEvent` published automatically by Spring Security's `AuthenticationManager`. | Decouples the lockout mechanism from the authentication flow. The provider doesn't need to know about lockout recording — an event listener handles it asynchronously. |
| **`JdbcUserDetailsManager`** | Spring Security's built-in JDBC implementation of `UserDetailsService` + `UserDetailsManager`. Reads from `users` and `authorities` tables. | No custom user entity or repository needed. The standard schema is well-understood, battle-tested, and compatible with all Spring Security features. |
| **Liquibase (Raw SQL Changesets)** | Schema management for the OAuth2 tables (`oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`) and Spring Security tables (`users`, `authorities`). | Schema is version-controlled and DBA-reviewable. Migrations run automatically on startup — no manual DDL. |
| **JWK (JSON Web Key) with RSA 2048** | RSA key pair generated at startup, wrapped in a `JWKSet`, and served at `/oauth2/jwks`. Used for signing JWTs. | Resource servers fetch this public key to validate token signatures locally. No shared secret, no call to the auth server per request. Asymmetric cryptography enables zero-trust token validation. |
| **HikariCP Connection Pool** | `minimum-idle: 5`, `maximum-pool-size: 20`, `connection-timeout: 30000`. | Production-ready connection pool. Min idle prevents cold-start latency; max pool prevents database exhaustion under load. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Builder Pattern** | `RegisteredClient.withId()`, `TokenSettings.builder()`, `ClientSettings.builder()`, `AuthorizationServerSettings.builder()` — fluent construction of immutable configuration objects with many optional parameters. |
| **Template Method (DaoAuthenticationProvider)** | `LockoutAwareAuthenticationProvider` overrides `authenticate()` to add a pre-check, then delegates to the parent's template method for actual credential verification. |
| **Observer / Event Listener** | `AuthenticationEventListener` observes authentication events published by the framework. Lockout recording is decoupled from the authentication flow — Single Responsibility Principle. |
| **Strategy (Client Authentication Methods)** | `ClientAuthenticationMethod.NONE` for public clients vs. `CLIENT_SECRET_BASIC` for confidential clients. The authorization server selects the verification strategy based on the registered client's configuration. |
| **Repository Pattern** | `RegisteredClientRepository`, `OAuth2AuthorizationService`, `OAuth2AuthorizationConsentService` — data access abstracted behind interfaces. JDBC implementations are injected; could be swapped for Redis-backed implementations. |
| **Idempotent Initialization** | `if (repository.findByClientId(...) == null) { repository.save(...); }` — clients are registered only if absent. Safe for repeated restarts without duplicate key errors. |
| **Configuration as Code (Record-Based Properties)** | `AuthServiceProperties` is an immutable Java record tree. The entire auth server configuration is type-safe, validated, and documented in one place — no stringly-typed property lookups scattered across the codebase. |
| **ConcurrentHashMap-Based State** | `AccountLockoutService` uses `ConcurrentMap.compute()` for atomic read-modify-write operations on lockout state. Thread-safe without explicit locks. |
| **Defense in Depth** | Multiple security layers: BCrypt password hashing → brute-force lockout → PKCE for public clients → short-lived tokens → revocation support. Each layer mitigates a different attack vector. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)` | Applies all default OAuth2 endpoint security (token, authorize, revoke, jwks, introspect) to the HttpSecurity builder |
| `OAuth2AuthorizationServerConfigurer.oidc(Customizer.withDefaults())` | Enables OpenID Connect 1.0 endpoints (userinfo, client registration, logout) |
| `RegisteredClient.withId(UUID)` | Builder entry point for creating an OAuth2 client registration |
| `AuthorizationGrantType.AUTHORIZATION_CODE` / `CLIENT_CREDENTIALS` | Enum constants defining which OAuth2 flows a client supports |
| `ClientAuthenticationMethod.NONE` | Indicates a public client (no secret). Combined with `requireProofKey(true)` for PKCE. |
| `OAuth2TokenType.ACCESS_TOKEN` | Used in the token customizer to target only access tokens (not ID tokens or refresh tokens) |
| `@ConfigurationProperties(prefix = "auth-server")` | Binds all `auth-server.*` YAML properties to the record hierarchy |
| `@Validated` on record | Triggers Bean Validation on the entire properties tree at startup |
| `JWKSet` / `RSAKey.Builder` / `ImmutableJWKSet` | Nimbus JOSE+JWT classes for constructing and serving JWKS |
| `ConcurrentHashMap.compute()` | Atomic read-modify-write for lockout state transitions |
| `@EventListener` | Declarative event handling — no manual event registration or interface implementation |
| `LoginUrlAuthenticationEntryPoint` | Redirects unauthenticated browser requests to `/login` for interactive auth |
| `MediaTypeRequestMatcher(MediaType.TEXT_HTML)` | Only applies the redirect entry point to browser requests (HTML), not API calls (JSON) |

### Concepts to Study Further

1. **RFC 7009 (Token Revocation) and Why JDBC Storage is Required** — Token revocation works by looking up the token in persistent storage and marking it invalid. In-memory token stores lose revocations on restart. Study the difference between token revocation (invalidating a specific token) vs. key rotation (invalidating all tokens signed with a key) vs. short TTL (relying on natural expiration). Understand why refresh token revocation is more critical than access token revocation given short access token lifetimes.

2. **Spring Authorization Server Filter Chain Architecture** — Two `SecurityFilterChain` beans coexist with different `@Order` values. Spring Security's `FilterChainProxy` matches each request against chains in order, delegating to the first match. The authorization server chain handles OAuth2 protocol endpoints; the default chain handles form login. Study how `RequestMatcher` determines which chain processes a request, and what happens when no chain matches.

3. **JWK Key Management in Production** — The current implementation generates a new RSA key pair on every startup (suitable for development). In production, keys must be persistent (loaded from a keystore, HSM, or secrets manager), rotatable (old keys remain valid for verification during rotation window), and the `kid` (key ID) must be stable across restarts. Study JWKS caching behavior in resource servers, key rotation strategies, and the `NimbusJwtDecoder` cache refresh mechanism.

---

## Task 4.3: Implement Profile-Based IdP Switching Mechanism

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **`@ConditionalOnProperty`** | A Spring Boot condition annotation that controls whether a `@Configuration` class or `@Bean` method is registered in the application context. The bean is only created when the specified property matches the expected value. | Enables feature toggles and provider switching without code changes. Setting `idp.provider=spring` activates the embedded auth server; setting `idp.provider=keycloak` deactivates it entirely — same binary, different behavior. |
| **`@ConfigurationProperties` with Java Records** | Binds externalized YAML/environment properties to an immutable Java record. The `IdpProperties` record captures `idp.provider` and `idp.jwks-uri` as validated, type-safe fields. | Configuration becomes a compile-time contract. Injecting `IdpProperties` anywhere in the codebase gives type-safe access to the active IdP settings — no string-based property lookups, no typos. |
| **`@EnableConfigurationProperties`** | Registers a `@ConfigurationProperties`-annotated class as a Spring bean without requiring `@Component` on the properties class itself. Used in `IdpAutoConfiguration` and `AuthorizationServerConfig`. | Keeps configuration classes clean — they don't need stereotype annotations. The enabling class controls when and where the properties are available. |
| **Spring Boot Auto-Configuration (`AutoConfiguration.imports`)** | The `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file lists configuration classes that Spring Boot should automatically register when the containing JAR is on the classpath. | Any service that depends on `common-lib` automatically gets `IdpProperties` registered — zero explicit import needed. This is how Spring Boot starters work internally. |
| **Property Placeholder Resolution (`${...}`)** | Spring's `PropertySourcesPlaceholderConfigurer` resolves `${idp.jwks-uri}` in YAML by looking up the `idp.jwks-uri` property value — which itself may resolve from an environment variable via `${IDP_JWKS_URI:default}`. | Creates a two-level indirection: environment variable → idp property → Spring Security config. The JWKS URI is set once and referenced everywhere, making provider switching a single-property change. |
| **Environment Variable Fallback (`${VAR:default}`)** | Spring Boot's property resolution supports default values: `${IDP_PROVIDER:spring}` means "use the `IDP_PROVIDER` env var, or fall back to `spring`." | Enables container orchestration (Docker Compose, Kubernetes) to inject provider choice at deploy time. Local development uses defaults without any env vars set. |
| **Spring Security OAuth2 Resource Server (JWKS validation)** | `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` tells Spring Security where to fetch the public keys for JWT signature verification. Works identically regardless of who issued the token. | The gateway and all resource servers validate tokens the same way — via the JWKS endpoint. They don't know or care whether Keycloak or Spring Authorization Server issued the token. This decouples token validation from token issuance. |
| **Bean Validation (`@NotBlank`) on Configuration Properties** | The `@Validated` annotation on `IdpProperties` triggers JSR 380 validation at application startup. If `idp.provider` or `idp.jwks-uri` is blank/missing, startup fails immediately with a clear error. | Fast failure with actionable error messages. A misconfigured IdP property fails at startup, not at the first user authentication attempt 10 minutes later. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Strategy Pattern (Provider Switching)** | The `idp.provider` property selects which identity provider strategy is active. `@ConditionalOnProperty` ensures only the selected provider's beans are instantiated. The resource server (gateway) is the Context — it validates tokens regardless of which Strategy issued them. |
| **Inversion of Control (IoC) via Conditional Beans** | The decision of which beans exist is externalized to configuration. Application code never checks `if (provider == "keycloak")` — Spring's container decides at startup which beans to create based on property values. |
| **Abstraction / Indirection (JWKS URI)** | Resource servers don't hardcode a provider-specific URL. They reference `${idp.jwks-uri}`, which resolves to the appropriate endpoint. The indirection means switching providers is a configuration change, not a code change. |
| **Auto-Configuration (Library Convention)** | `IdpAutoConfiguration` follows the Spring Boot starter convention: declare in `AutoConfiguration.imports` → available in all dependent services automatically. This is how first-party and third-party starters distribute cross-cutting configuration. |
| **Separation of Concerns** | Token issuance (auth-service) is completely separate from token validation (gateway, business services). The only coupling is the JWKS URI — a public endpoint returning public keys. Either side can be replaced independently. |
| **Null Object / No-Op Pattern** | When `idp.provider=keycloak`, the auth-service starts but registers no authorization server beans. It becomes a no-op for token issuance while still participating in service discovery. No special "disabled" code path — the conditional simply prevents bean creation. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")` | Only creates beans when the property matches. The auth server configuration classes use this to activate exclusively for the Spring provider. |
| `@ConfigurationProperties(prefix = "idp")` | Binds all `idp.*` properties to the `IdpProperties` record. |
| `@EnableConfigurationProperties(IdpProperties.class)` | Registers `IdpProperties` as a bean from within a `@Configuration` class. |
| `@Validated` | Triggers Bean Validation on the configuration properties record at startup. |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Spring Boot 3.x+ mechanism for registering auto-configuration classes (replaces the old `spring.factories` approach). |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | Spring Security property configuring where to fetch JWKS for JWT validation. Accepts property placeholders. |
| `${IDP_JWKS_URI:http://localhost:8090/oauth2/jwks}` | Environment variable with default — the primary configuration hook for switching JWKS endpoints. |

### Concepts to Study Further

1. **Spring Boot Conditional Annotations Deep Dive** — Beyond `@ConditionalOnProperty`, Spring Boot provides `@ConditionalOnClass`, `@ConditionalOnBean`, `@ConditionalOnMissingBean`, `@ConditionalOnWebApplication`, and `@ConditionalOnExpression`. These form the backbone of auto-configuration. Study how Spring evaluates conditions during the `ConfigurationClassPostProcessor` phase, before bean instantiation, and how `@Conditional` (the meta-annotation) allows writing custom conditions via `Condition.matches()`. Understanding this mechanism explains how Spring Boot starters achieve "magic" auto-wiring.

2. **JWKS Caching, Rotation, and Failover** — When a resource server fetches JWKS, it caches the keys. If the IdP rotates keys (new `kid`), the resource server must re-fetch. Spring Security's `NimbusJwtDecoder` handles this via a `JWKSetCache` with configurable TTL. Study what happens during key rotation (both old and new keys are served simultaneously), what happens if the JWKS endpoint is temporarily unreachable (cached keys still work until TTL expires), and how to implement JWKS failover with multiple IdP instances behind a load balancer.

3. **Multi-Tenancy and Multi-IdP Patterns** — This implementation switches between two IdPs at deployment time (one active at a time). More advanced patterns include: runtime multi-IdP (validate tokens from multiple issuers simultaneously using `JwtIssuerAuthenticationManagerResolver`), multi-tenancy (different tenants use different IdPs), and federated identity (one IdP trusts another via SAML or OIDC federation). Study Spring Security's `AuthenticationManagerResolver` for request-time IdP selection.

---

## Task 4.4: Write Integration Tests for Authentication Flows

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **`@SpringBootTest(webEnvironment = RANDOM_PORT)`** | Boots the full application context (authorization server, security, JPA, Liquibase) on a random available port. Tests execute against the real HTTP stack, not mock controllers. | Validates the complete request-response lifecycle — CSRF handling, session cookies, filter chains, token issuance, database persistence — exactly as it works in production. |
| **Testcontainers PostgreSQL 16** | Spins up a real PostgreSQL 16 container for each test class. Liquibase migrations run against it, creating the OAuth2 and Spring Security tables. | No H2 or in-memory substitutes. Tests validate real SQL dialect behavior, constraint enforcement, and JDBC compatibility — catches issues like PostgreSQL-specific type handling that H2 would mask. |
| **`@DynamicPropertySource`** | Injects the Testcontainers-provided JDBC URL, username, and password into the Spring `Environment` at context startup. Also sets `eureka.client.enabled=false` and `idp.provider=spring`. | Bridges the gap between container startup (random port/credentials) and Spring context initialization. Ensures the application connects to the test database, not a default/local instance. |
| **`TestRestTemplate`** | Spring Boot's test-friendly `RestTemplate` wrapper that follows redirects, manages cookies, and handles HTTP Basic auth fluently. | Simulates real HTTP clients (browsers, service consumers) interacting with the authorization server. Unlike `MockMvc`, it exercises the full Netty/Tomcat stack including all filters. |
| **`@LocalServerPort`** | Injects the randomly assigned port into the test class. Combined with a `baseUrl()` helper for constructing endpoint URLs. | Eliminates port conflicts when tests run in parallel. Each test class gets its own server instance on a unique port. |
| **Spring Security Test Support (`spring-security-test`)** | Provides utilities for testing security configurations, though here we primarily use `TestRestTemplate` for full HTTP-level testing rather than mock security contexts. | Enables integration with Spring Security's test infrastructure for scenarios like pre-authenticated mock users when needed alongside full HTTP tests. |
| **JUnit 5 `@ParameterizedTest` + `@ValueSource`** | Runs the same test method multiple times with different input values. Used for password policy tests to verify rejection of various weak passwords. | Concise coverage of multiple failure cases without duplicating test methods. Each input is reported separately in test output for easy debugging. |
| **`@DisplayName`** | Human-readable test names shown in IDE and CI reports instead of method names. | Makes test intent immediately clear. `"Should lock account after 5 failed login attempts"` is more informative than `shouldLockAfterFiveFailedAttempts`. |
| **`@BeforeEach`** | Per-test setup method. Used to reset lockout state, generate PKCE parameters, and provision test users. | Test isolation — each test starts from a clean state. No test-order dependencies or state leakage between tests. |
| **`JdbcUserDetailsManager`** | Spring Security's JDBC implementation for user CRUD operations. Used in tests to create PMO/POC users with specific roles for RBAC verification. | Provisions test users programmatically without Keycloak. Validates that the `users` and `authorities` tables (managed by Liquibase) work correctly with the Spring Security user management API. |
| **PKCE (RFC 7636) Implementation in Tests** | Test generates `code_verifier` (random 32 bytes, Base64URL-encoded) and `code_challenge` (SHA-256 hash of verifier, Base64URL-encoded). Sent during authorization and token exchange. | Validates that the authorization server correctly enforces PKCE for public clients. The test proves the cryptographic binding between the authorization request and the token exchange. |
| **Base64URL Decoding for JWT Inspection** | Tests decode JWT payloads (the middle segment between dots) using `Base64.getUrlDecoder()` to inspect claims without a full JWT library. | Lightweight claim verification. Confirms `iss`, `sub`, `realm_access.roles`, and token structure without needing Nimbus JOSE+JWT in test dependencies. |
| **HTTP Basic Auth (`headers.setBasicAuth()`)** | Constructs the `Authorization: Basic <base64(clientId:clientSecret)>` header for the Client Credentials flow and token introspection/revocation. | Tests the OAuth2 client authentication mechanism exactly as backend services would authenticate — verifying that invalid secrets are properly rejected with 401. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Template Method (Base Test Class)** | `BaseAuthIntegrationTest` defines the shared setup (Testcontainers, property injection, `baseUrl()`) while concrete test classes override only the test methods. Eliminates boilerplate across 7 test files. |
| **Test Fixture / Object Mother** | `generatePkce()` in `@BeforeEach` creates fresh PKCE verifier/challenge pairs for each test. `setupTestUsers()` provisions PMO/POC users. Factory-style setup ensures each test has well-defined input state. |
| **Test Isolation via Reset** | `AccountLockoutIntegrationTest.resetLockoutState()` calls `lockoutService.resetAttempts()` before each test. Prevents one test's 5 failed attempts from affecting the next test's lockout assertion. |
| **Arrange-Act-Assert** | Each test follows the pattern: set up preconditions (arrange), execute the operation (act), verify the outcome (assert). Example: record 5 failures → check `isLocked()` → assert true. |
| **Indirect Verification** | RBAC tests verify role mapping by loading `UserDetails` and inspecting authorities. Since full browser-based Authorization Code flow is complex to automate in tests, the role verification confirms the data that the `OAuth2TokenCustomizer` would include in JWTs. |
| **Idempotent Revocation Testing** | Token revocation is tested for idempotency — revoking the same token twice should both return 200. This validates RFC 7009 compliance (revocation is always successful, even for unknown tokens). |
| **CSRF Token Extraction** | The lockout test extracts CSRF tokens from the login page HTML using regex patterns. This validates that the full security filter chain (including CSRF protection) is active during tests — not bypassed by test shortcuts. |
| **Boundary Testing** | Lockout tests verify behavior at the boundary: 4 attempts = not locked (boundary - 1), 5 attempts = locked (boundary). Password tests verify exactly 11 chars = rejected, 12+ chars with all requirements = accepted. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` | Full integration test with embedded server on random port |
| `@Testcontainers` | JUnit 5 extension managing Testcontainers lifecycle |
| `@Container static PostgreSQLContainer<?>` | Shared PostgreSQL container (started once per class) |
| `@DynamicPropertySource` | Injects container properties into Spring Environment |
| `@LocalServerPort` | Injects the random port for URL construction |
| `TestRestTemplate.postForEntity()` | Executes HTTP POST and returns full `ResponseEntity` |
| `HttpHeaders.setBasicAuth(clientId, secret)` | Constructs OAuth2 client authentication header |
| `LinkedMultiValueMap` | Spring's multivalue map for form-encoded request bodies |
| `Base64.getUrlDecoder().decode(jwtPart)` | Decodes JWT payload segment for claim inspection |
| `MessageDigest.getInstance("SHA-256")` | Computes PKCE code_challenge from code_verifier |
| `assertThat(...).isIn(200, 302)` | AssertJ assertion allowing multiple valid status codes |
| `JdbcUserDetailsManager.createUser()` | Provisions test users with specific roles in the database |
| `AccountLockoutService.resetAttempts()` | Clears lockout state for test isolation |
| `@ParameterizedTest` + `@ValueSource(strings = {...})` | Data-driven tests with multiple weak password inputs |

### Concepts to Study Further

1. **OAuth2 Grant Type Testing Strategies** — The Authorization Code + PKCE flow is inherently interactive (requires browser redirect, login form, consent). In integration tests, this is simulated by extracting session cookies and CSRF tokens programmatically. Study alternatives: Spring Security's `OAuth2AuthorizationRequestRedirectFilter` test utilities, `MockMvc` with `SecurityMockMvcRequestPostProcessors.oidcLogin()`, and contract testing with Pact for service-to-IdP interactions. Understand when full HTTP tests are necessary vs. when mock-based tests suffice.

2. **Token Introspection vs. JWT Self-Contained Validation** — The revocation test uses the `/oauth2/introspect` endpoint to confirm a token is invalidated. This raises an important architectural question: JWTs are self-contained (verifiable without calling the IdP), so how does revocation work? Study the trade-off between short TTL (natural expiration) vs. token introspection (real-time validation with extra round-trip) vs. revocation lists (CRL-style). Understand why the test uses introspection to verify revocation, and why production systems often rely on short access token TTL + refresh token revocation instead.

3. **Testcontainers Lifecycle and Container Reuse** — The `@Container static` annotation means one PostgreSQL container per test class. With 7 test classes, that's 7 container startups (~3-5s each). Study Testcontainers' `reuse` feature (`withReuse(true)` + `.testcontainers.properties`), `@SharedContainerGroup` patterns, and the Singleton Container pattern for sharing a single container across all test classes. Also study `@DynamicPropertySource` limitations with bean refresh — properties are set once at context startup and cannot change mid-test.

---

## Task 5: Checkpoint — Infrastructure and Auth Tests Pass

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **`JdbcOAuth2AuthorizationService` Row Mapper Customization** | Spring Authorization Server stores active authorizations (tokens, codes) in a JDBC table. The row mapper deserializes the `attributes` column (JSON) back into Java objects using Jackson. A custom `ObjectMapper` can be set via `setAuthorizationRowMapper()`. | Without proper Jackson module registration, stored authorization attributes containing Java immutable collections (`Map.of()`, `List.of()`) fail deserialization with an allowlist error. Customizing the row mapper is essential for JDBC-backed authorization servers. |
| **`SecurityJackson2Modules.getModules(ClassLoader)`** | Returns all Spring Security Jackson modules (for `UsernamePasswordAuthenticationToken`, `SimpleGrantedAuthority`, etc.) registered via `META-INF/services`. | Spring Security uses polymorphic deserialization with a strict allowlist. Without these modules, any Security-typed object stored in the authorization table cannot be deserialized. |
| **`OAuth2AuthorizationServerJackson2Module`** | Jackson module registering mixins for OAuth2 Authorization Server types (`OAuth2Authorization`, `OAuth2AccessToken`, registered client settings, etc.). | Token metadata, client settings, and authorization attributes all use custom Jackson serialization. This module provides the required mixins and deserializers. |
| **`ObjectMapper.activateDefaultTyping()`** | Enables polymorphic type handling in Jackson — each serialized object includes a `@class` type identifier that Jackson uses during deserialization to instantiate the correct concrete type. | When Spring Security stores an `Authentication` object containing a `Map.of(...)` (which is `java.util.ImmutableCollections$Map1` at runtime), Jackson needs to know which concrete class to instantiate on read. Default typing enables this. |
| **Spring Authorization Server Token Issuance Rules** | For public clients (`ClientAuthenticationMethod.NONE`), Spring Authorization Server 1.4.x does NOT issue refresh tokens — even when `AuthorizationGrantType.REFRESH_TOKEN` is registered. This is a deliberate security decision in `OAuth2AuthorizationCodeAuthenticationProvider`. | Public clients (SPAs) cannot securely store or rotate refresh tokens. The server enforces this at the framework level. Confidential clients (service-to-service) do receive refresh tokens. Understanding framework-enforced security policies prevents misguided configuration changes. |
| **Maven Reactor with `-pl` (project list)** | `mvnw test -pl common-lib,config-server,discovery-service,gateway-service,auth-service` builds/tests only the specified modules in dependency order. | Fast feedback during checkpoints — no need to compile services that haven't changed. The reactor resolves inter-module dependencies automatically. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Allowlist-Based Deserialization** | Spring Security's `SecurityJackson2Modules` uses an allowlist (`AllowlistTypeIdResolver`) instead of allowing arbitrary class deserialization. This prevents Remote Code Execution (RCE) via Jackson polymorphic deserialization gadgets. The fix registers the proper modules so that legitimate types are allowlisted. |
| **Layered ObjectMapper Configuration** | The authorization service ObjectMapper is configured in layers: (1) base ObjectMapper, (2) Spring Security modules (authentication types), (3) OAuth2 Authorization Server module (token types), (4) default typing activation. Each layer adds capabilities without conflicting with others. |
| **Security by Default (Restrictive Token Issuance)** | Spring Authorization Server's decision to NOT issue refresh tokens for public clients is an example of "secure by default" — the framework assumes the safest behavior unless explicitly overridden. This shifts the burden of proof to developers who want less-secure configurations. |
| **Checkpoint Verification (Quality Gate)** | The checkpoint task itself is a quality gate pattern — no downstream tasks can begin until all infrastructure and auth tests pass. This prevents building on a broken foundation and catching integration issues early in the DAG. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `JdbcOAuth2AuthorizationService.setAuthorizationRowMapper()` | Overrides the default row mapper with a custom-configured instance |
| `OAuth2AuthorizationRowMapper.setObjectMapper()` | Injects a custom Jackson ObjectMapper for deserialization |
| `SecurityJackson2Modules.getModules(classLoader)` | Discovers all Spring Security Jackson modules on the classpath |
| `OAuth2AuthorizationServerJackson2Module` | Jackson module for OAuth2 Authorization Server type mixins |
| `ObjectMapper.activateDefaultTyping(validator, typing, idStyle)` | Enables `@class` type metadata in serialized JSON for polymorphic deserialization |
| `PolymorphicTypeValidator` | Controls which classes are permitted for polymorphic deserialization |
| `ClientAuthenticationMethod.NONE` | Designates a public client — no client secret, must use PKCE |
| `RegisteredClient.authorizationGrantType(REFRESH_TOKEN)` | Registers the grant type but doesn't guarantee issuance for all client types |

### Concepts to Study Further

1. **Jackson Polymorphic Deserialization Security** — The `ImmutableCollections$Map1` error reveals a fundamental Jackson security mechanism. When `defaultTyping` is enabled, Jackson writes `"@class": "java.util.ImmutableCollections$Map1"` into JSON. On deserialization, it instantiates that class. Without an allowlist, an attacker could inject `"@class": "com.evil.Exploit"` and achieve RCE. Study CVE-2017-7525, the `PolymorphicTypeValidator` API, and why Spring Security uses a strict allowlist (`AllowlistTypeIdResolver`) instead of a denylist approach. Understand the trade-off between `LaissezFaireSubTypeValidator` (unsafe but convenient) and custom validators.

2. **OAuth2 Refresh Token Security Model** — Spring Authorization Server's refusal to issue refresh tokens for public clients reflects a real security concern: if a browser-based SPA stores a long-lived refresh token, XSS can exfiltrate it for persistent access. Study the alternatives: (a) Backend-for-Frontend (BFF) pattern where the SPA never touches tokens, (b) token rotation with sender-constrained tokens (DPoP), (c) sliding session via silent iframe re-auth. Understand why confidential clients (server-side) ARE safe to receive refresh tokens.

3. **Spring Context Caching in Integration Tests** — When multiple test classes share the same `@SpringBootTest` configuration (same properties, same profile), Spring reuses the ApplicationContext. This is desirable (faster tests) but creates coupling: if one test class mutates shared state (database, cache, in-memory stores), subsequent classes see stale data. Study `@DirtiesContext` (forces fresh context — slow), shared static containers (one container, one context), and `TestExecutionListener` for advanced cleanup strategies.


---

## Task 6.1: Create Liquibase Migration Scripts (PostgreSQL)

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Liquibase with Raw SQL Changesets** | Database migration tool that tracks and applies schema changes via versioned scripts. Using `--liquibase formatted sql` header in `.sql` files enables raw SQL while retaining Liquibase metadata (changeset IDs, rollback blocks). | DBA-reviewable migrations — they read plain SQL, not XML/YAML/JSON abstractions. The `DATABASECHANGELOG` table provides an auditable history of what was applied and when. |
| **Master Changelog XML** | `db.changelog-master.xml` uses `<include file="..."/>` to reference individual SQL scripts in execution order. | Single entry point for Liquibase. The XML orchestrates ordering while individual SQL files remain focused on a single concern (one table per file). Separation enables parallel development. |
| **PostgreSQL Extensions (`CREATE EXTENSION`)** | `pgcrypto` provides `gen_random_uuid()` for UUID generation. `pg_trgm` provides trigram-based similarity matching and GIN index support. | Extensions are PostgreSQL's plugin mechanism. `pg_trgm` enables fuzzy/partial text matching via `%` and `similarity()` operators — far more flexible than LIKE queries. |
| **`GENERATED ALWAYS AS IDENTITY`** | PostgreSQL's standards-compliant auto-increment mechanism (SQL:2003). Alternative to `SERIAL` pseudo-type. | Unlike `SERIAL` (which creates a hidden sequence + default), `IDENTITY` is an explicit column property. It prevents accidental manual inserts that desynchronize the sequence. |
| **`gen_random_uuid()` Default** | PostgreSQL 13+ built-in function generating UUIDv4 values without requiring `pgcrypto`. | UUID primary keys enable globally-unique identifiers without coordination. Useful in distributed systems where multiple services/instances write to the same table. |
| **`TIMESTAMP WITH TIME ZONE`** | PostgreSQL stores the timestamp in UTC internally, converting to/from the session's `timezone` setting on read/write. | Eliminates timezone ambiguity. All services see the same instant regardless of their JVM timezone. Critical for distributed systems spanning multiple regions. |
| **CHECK Constraints** | `CONSTRAINT "chk_users_role" CHECK ("role" IN ('ADMIN', 'PMO', 'POC'))` — database-level enum enforcement. | Defense in depth: even if application validation is bypassed (direct SQL, migration script, admin tool), the database rejects invalid data. The constraint is a last line of defense. |
| **GIN Indexes with `gin_trgm_ops`** | Generalized Inverted Indexes using trigram operator class. Supports `LIKE '%partial%'`, `ILIKE`, and similarity queries efficiently. | B-tree indexes only support left-anchored LIKE (`foo%`). GIN+pg_trgm indexes support substring matching anywhere in the string — enabling real search without a dedicated search engine. |
| **Optimistic Locking Column (`version BIGINT DEFAULT 0`)** | A counter incremented by JPA `@Version` on each update. Concurrent conflicting updates detect the stale version. | The database schema must include this column for JPA optimistic locking to work. It's a schema concern (DDL) not just an application concern. |
| **Rollback SQL** | Each changeset includes `--rollback DROP TABLE IF EXISTS "..."` or equivalent reverse DDL. | Enables `liquibase rollback` for failed deployments. Without rollback SQL, Liquibase can't undo changesets — requiring manual intervention during failed releases. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Migration-Per-Concern** | Each SQL file addresses a single table or logical concern (extensions, users, events, indexes). This makes migrations independently reviewable, testable, and rollback-able. |
| **Naming Convention as Contract** | `YYYYMMDD-NNN-description.sql` provides natural ordering (date prefix), disambiguation within a day (sequence number), and human readability (description). The naming IS the ordering — Liquibase processes includes in the order listed in the master changelog. |
| **Defense in Depth (Constraints)** | Data integrity is enforced at multiple layers: application validation (Bean Validation), JPA constraints (`@Column(nullable=false)`), and database CHECK/UNIQUE/FK constraints. The database is the ultimate authority. |
| **Single Responsibility per Migration** | Each file creates exactly one table. This means a migration failure identifies exactly which table has the issue. Contrast with a monolithic migration where one syntax error blocks all table creation. |
| **Composite Keys for Junction Tables** | `event_beneficiary` uses a composite primary key `(event_id, beneficiary_id)` — the relationship IS the identity. No surrogate key needed because the combination is inherently unique and meaningful. |

### Key Annotations & APIs

| Concept / Syntax | Purpose |
|-----------------|---------|
| `--liquibase formatted sql` | File header marking this as a Liquibase-managed SQL changeset file |
| `--changeset author:id` | Declares a new changeset with a unique author:id combination tracked in DATABASECHANGELOG |
| `--rollback SQL_STATEMENT` | Defines the reverse operation for `liquibase rollback` |
| `--comment: description` | Human-readable description stored in DATABASECHANGELOG |
| `CREATE EXTENSION IF NOT EXISTS` | Idempotent extension activation — safe to re-run |
| `CONSTRAINT ... FOREIGN KEY ... REFERENCES ... ON DELETE CASCADE` | Referential integrity with cascade behavior |
| `COMMENT ON TABLE/COLUMN` | PostgreSQL metadata comments visible in `\d+` and admin tools |

### Concepts to Study Further

1. **Liquibase vs. Flyway — When to Choose Which** — Both are migration tools, but they differ in philosophy. Flyway is "convention over configuration" (numbered scripts, no XML). Liquibase offers changelogs, contexts, labels, preconditions, and multi-database support. Study when the extra complexity of Liquibase pays off (multi-environment, conditional execution) vs. when Flyway's simplicity wins.
2. **PostgreSQL GIN Indexes and pg_trgm Internals** — A GIN index stores an inverted index of trigrams (3-character subsequences). `"hello"` → `{"hel", "ell", "llo"}`. Queries find rows whose trigram set overlaps the search term's trigrams. Study the space-time trade-off: GIN indexes are large but enable sub-string search without full table scans. Compare with GiST indexes (lossy, smaller, slower).
3. **UUID as Primary Key — Performance Implications** — UUIDv4 is random, which means B-tree index page splits are random (not append-only like auto-increment). This can degrade write performance on large tables. Study UUIDv7 (time-ordered), ULID, and PostgreSQL's `uuid_generate_v7()` (PG 17+) as alternatives that provide both global uniqueness and insert-order locality.

---

## Task 6.2: Configure MongoDB Collections and Indexes

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **`@EventListener(ApplicationReadyEvent.class)`** | Executes a method after the Spring Boot application is fully started (all beans created, server listening). | Collection/index initialization happens after the MongoDB connection is established, after health checks pass, and after the application is ready to serve traffic. Avoids startup ordering issues. |
| **`MongoTemplate.indexOps(collectionName).ensureIndex()`** | Programmatic index creation using Spring Data MongoDB's fluent API. Idempotent — if the index exists with the same definition, it's a no-op. | Ensures indexes exist on every startup without errors on re-runs. Unlike `createIndex()` which throws on duplicates, `ensureIndex()` is safe for repeated execution. |
| **TTL Indexes (`Index.expire(Duration)`)** | MongoDB automatically deletes documents when the indexed field's value exceeds the specified duration from the current time. | Automated data lifecycle management. Audit logs, domain events, and job tracking records self-clean without application-level batch jobs or cron schedules. |
| **`MongoClientSettingsBuilderCustomizer`** | A functional interface that Spring Boot invokes when building the `MongoClient`. Allows programmatic customization of connection pool, socket, and cluster settings. | The `MongoClient` is built by Spring Boot's auto-configuration. Customizers hook into that process without replacing the entire auto-config. Multiple customizers compose cleanly. |
| **`@ConfigurationProperties(prefix = "spring.data.mongodb")`** | Binds all YAML properties under a prefix to a typed Java bean. Spring Boot validates and injects values before application startup. | Type-safe pool configuration. `minConnectionsPerHost` binds from YAML; typos cause startup failures, not silent defaults. |
| **Spring Data MongoDB Auto-Index Creation** | `spring.data.mongodb.auto-index-creation: true` tells Spring Data to create indexes defined by `@Indexed` annotations on document classes. | Declarative index management for entities annotated with `@Document`. Complements the programmatic `MongoDbInitializer` which handles collections without corresponding entity classes. |
| **HikariCP Connection Pool (PostgreSQL)** | `minimum-idle: 5, maximum-pool-size: 20, connection-timeout: 5000` — bounded pool with fast failure on exhaustion. | Prevents database connection exhaustion under load. `minimum-idle` avoids cold-start latency; `maximum-pool-size` prevents unbounded connection growth that could overwhelm the database. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Initializer Pattern** | `MongoDbInitializer` is a dedicated component whose sole responsibility is ensuring database prerequisites on startup. Separates infrastructure setup from business logic. |
| **Idempotent Operations** | Both `createCollection` (checks existence first) and `ensureIndex` (no-op if exists) are idempotent. The application can restart any number of times without duplicate errors. Essential for container orchestration where restarts are normal. |
| **Builder Pattern (Fluent Index API)** | `new Index().on("field", Direction.ASC).on("field2", Direction.DESC).expire(duration).named("name")` — method chaining constructs complex index definitions readably. |
| **Constants as Documentation** | `COLLECTION_DOMAIN_EVENTS = "domain_events"` — named constants document what collections exist, enable compile-time reference checking, and prevent typo-based bugs. |
| **Dual-Mode Initialization** | Both a standalone `init-collections.js` (for DBA/ops use with `mongosh`) and a Spring component (for application-managed setup) exist. Ops teams can pre-create collections in production; the app gracefully handles either state. |
| **Connection Pool Sizing Strategy** | Min=5 keeps warm connections ready (no TCP handshake on first request). Max=20 bounds resource usage (each MongoDB connection consumes ~1MB RAM on the server). These defaults follow MongoDB's own recommendations for microservice architectures. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@EventListener(ApplicationReadyEvent.class)` | Triggers initialization after full application startup |
| `MongoTemplate.getCollectionNames()` | Lists existing collections for idempotent creation checks |
| `MongoTemplate.createCollection(name)` | Creates a new collection (throws if exists — checked beforehand) |
| `IndexOperations.ensureIndex(Index)` | Idempotent index creation — no-op if already exists |
| `Index.expire(Duration)` | Sets the TTL for automatic document expiration |
| `Index.named(String)` | Explicit index name (otherwise MongoDB auto-generates one) |
| `MongoClientSettingsBuilderCustomizer` | Hook for programmatic MongoClient configuration |
| `ConnectionPoolSettings.builder().minSize().maxSize()` | MongoDB driver pool sizing |
| `SocketSettings.builder().connectTimeout().readTimeout()` | MongoDB driver timeout configuration |

### Concepts to Study Further

1. **MongoDB TTL Index Mechanics** — TTL indexes run a background thread every 60 seconds that checks if documents are expired. This means actual deletion can lag up to 60 seconds behind the TTL expiry. Study: what happens if the clock skews between replica set members, why TTL indexes must be single-field (not compound), and how capped collections differ from TTL-based cleanup.
2. **Connection Pool Sizing for Microservices** — With 6 services each having `maxPoolSize=20`, the MongoDB server handles up to 120 connections from this platform alone. MongoDB's default `maxIncomingConnections` is 65536, but each connection consumes ~1MB RAM. Study: how to calculate pool size based on thread count and query latency (`pool_size ≥ max_concurrent_requests × avg_query_time / avg_request_time`), and why oversized pools waste resources while undersized pools cause queuing.
3. **MongoDB Index Selection and the Query Planner** — When a query matches multiple indexes, MongoDB's query planner runs a "race" — it tries multiple plans simultaneously and caches the winner. Study: compound index prefix rules (a compound index on `{a, b}` satisfies queries on `{a}` but not `{b}`), covered queries (index-only queries that never touch documents), and `explain("executionStats")` for production query optimization.

---

## Task 6.3: Write Integration Tests for Database Migrations

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Testcontainers (`@Testcontainers` + `@Container`)** | JUnit 5 extension that manages Docker container lifecycle tied to test class lifecycle. `@Container static` starts one container per class; instance-level starts per test method. | Real database instances in tests. No behavior differences between test and production — PostgreSQL and MongoDB run the same engine, same SQL dialect, same index behavior. |
| **`@DynamicPropertySource`** | Injects properties from running containers (JDBC URL, MongoDB URI) into the Spring Environment before context startup. | Solves the bootstrap problem: containers start on random ports, but Spring needs connection strings at context creation time. This bridges the two lifecycles. |
| **`@SpringBootTest` with Selective Auto-Configuration Exclusion** | `spring.autoconfigure.exclude` disables specific auto-configuration classes (e.g., exclude MongoDB when testing only PostgreSQL). | Isolates the system under test. The Liquibase migration test doesn't need MongoDB — excluding its auto-config eliminates a container dependency and speeds up the test. |
| **`JdbcTemplate` for Schema Verification** | Spring's template class for executing SQL queries. Used to query `information_schema` system catalog tables for metadata about tables, constraints, and indexes. | Tests verify the migration output, not the migration process. Querying `information_schema.tables`, `information_schema.table_constraints`, and `pg_indexes` confirms the schema matches expectations. |
| **`@ActiveProfiles("test")`** | Activates the `test` profile, loading `application-test.yml` which disables Eureka, security auto-config, and sets test-appropriate timeouts. | Isolates tests from infrastructure dependencies. No need for a running Eureka server or Keycloak instance during database migration tests. |
| **Classpath Resource Scanning** | `getClass().getClassLoader().getResource("db/changelog")` locates the migration directory on the classpath. Tests then verify file naming conventions against actual files. | Convention enforcement as a test. If a developer creates a migration file with incorrect naming, the CI build fails — not a production deployment. |
| **XML Document Parsing (`DocumentBuilderFactory`)** | Parses `db.changelog-master.xml` to extract `<include file="..."/>` elements and verify all SQL files are referenced. | Catches a common error: creating a migration file but forgetting to add it to the master changelog. The test guarantees completeness. |
| **`MongoClient.getDatabase().getCollection().listIndexes()`** | Low-level MongoDB driver API that returns raw BSON documents describing each index (name, key pattern, TTL settings, unique flag). | Tests verify index creation at the driver level — the same level that queries use to select indexes. Confirms TTL `expireAfterSeconds` values precisely. |
| **`SpringApplication` Programmatic Startup** | `new SpringApplication(class)` with `setDefaultProperties()` allows tests to start a context with deliberately broken configuration (unreachable hosts). | Verifies failure behavior: when PostgreSQL is unreachable, does the application fail with a descriptive error, or does it hang silently? |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Test Isolation via Container Scoping** | `LiquibaseMigrationIT` uses PostgreSQL only (excludes MongoDB auto-config). `MongoDbIndexIT` uses MongoDB only (excludes JPA/Liquibase). Each test class has minimal dependencies — failures are immediately attributable. |
| **Schema-as-Contract Testing** | Tests treat the database schema as a public contract. If a migration changes a table name, column type, or removes a constraint, the test fails — catching breaking changes before downstream services discover them at runtime. |
| **Convention Enforcement via Testing** | `MigrationFileNamingConventionTest` encodes the team's naming convention as an executable specification. New team members can't accidentally deviate because CI catches it. |
| **Negative Testing (Failure Path)** | `MissingConfigurationIT` deliberately provides broken configuration to verify the application fails correctly. This is more valuable than happy-path testing for production reliability — graceful failure with clear error messages reduces MTTR. |
| **Catalog-Driven Verification** | Rather than parsing SQL migration files (fragile), tests query the actual database catalog (`information_schema`, `pg_indexes`). This verifies the final state regardless of how it was achieved — resilient to migration refactoring. |
| **Regex-Based Convention Validation** | `Pattern.compile("^\\d{8}-\\d{3}-[a-z][a-z0-9-]+\\.sql$")` — the naming convention is encoded as a regex. Unambiguous, machine-checkable, and self-documenting. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@Testcontainers` | JUnit 5 extension managing container lifecycle |
| `@Container static PostgreSQLContainer<?>` | Shared PostgreSQL container started once per test class |
| `@Container static MongoDBContainer` | Shared MongoDB container started once per test class |
| `@DynamicPropertySource` | Injects container connection details into Spring context |
| `@ActiveProfiles("test")` | Loads test-specific configuration (no Eureka, no security) |
| `@Tag("integration")` | JUnit 5 tag for selective test execution (e.g., `mvn verify` via failsafe) |
| `information_schema.tables` | PostgreSQL system catalog listing all tables |
| `information_schema.table_constraints` | System catalog listing UNIQUE, CHECK, FK constraints |
| `pg_indexes` | PostgreSQL view listing all indexes with their definitions |
| `pg_extension` | System catalog listing installed extensions |
| `databasechangelog` | Liquibase tracking table recording applied changesets |
| `MongoCollection.listIndexes()` | Returns BSON documents describing each index on a collection |
| `Document.getLong("expireAfterSeconds")` | Extracts TTL value from MongoDB index metadata |

### Concepts to Study Further

1. **Testcontainers Singleton Pattern vs. Per-Class Containers** — `@Container static` creates one container per test class. With multiple test classes, multiple containers start sequentially. The Singleton Container pattern uses a base class with a static container shared across ALL test classes (started once per JVM). Study: when to use per-class (isolation) vs. singleton (speed), how `@DynamicPropertySource` works with inherited containers, and the Testcontainers `reuse` mode for development-time container persistence.
2. **PostgreSQL `information_schema` vs. `pg_catalog`** — `information_schema` is the SQL-standard metadata schema (portable across databases). `pg_catalog` is PostgreSQL-specific but more detailed (includes GIN/GiST index info, extension details, custom types). Study when to use which: `information_schema` for portable queries, `pg_catalog` for PostgreSQL-specific features like trigram indexes.
3. **Database Migration Testing Strategies in CI/CD** — Beyond "did migrations run?", production teams test: (a) migration idempotency (run twice — no errors), (b) migration performance (does a new index lock the table?), (c) backward compatibility (can old code run against new schema?), (d) data migration correctness (are existing rows transformed correctly?). Study blue-green deployment with migrations, expand-contract pattern, and zero-downtime schema changes.


---

## Tasks 7.1–7.7: Event Service (Full Implementation)

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **JPA Auditing (`@EnableJpaAuditing`)** | Auto-populates `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy` fields on entity persist/update. | Audit trail without manual code. The `auditorAwareRef` links to common-lib's `SecurityAuditorAware` which extracts the user from the security context. |
| **Optimistic Locking (`@Version`)** | A version counter incremented on each update. Concurrent conflicting writes throw `ObjectOptimisticLockingFailureException`. | Prevents lost updates without database-level pessimistic locks. The integration test verifies concurrent PUT requests produce 409 Conflict. |
| **`@ConfigurationProperties` with Java Records** | Type-safe, immutable configuration binding using `@DefaultValue` and Bean Validation annotations. | Configuration becomes a compile-time contract. Invalid values fail at startup, not at the first request. |
| **Spring Scheduling (`@EnableScheduling` + `@Scheduled`)** | Executes methods at fixed intervals. Used for the domain event outbox poller (every 5 seconds). | Lightweight alternative to a message broker for event-driven architecture. The poller processes PENDING events and marks them PUBLISHED. |
| **Spring Data MongoDB (`MongoTemplate` + `MongoRepository`)** | Document persistence for domain events, audit logs. `MongoTemplate` provides low-level query control; repositories provide CRUD. | MongoDB fits append-only workloads (audit logs, event outbox) where schema flexibility and write performance matter more than referential integrity. |
| **`@RestControllerAdvice` with `ProblemDetail`** | Global exception handling returning RFC 7807 Problem Detail responses. | Standardized error format. Clients always get `{type, title, status, detail}` — no custom error schemas needed. |
| **MapStruct with `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)`** | Partial updates: only non-null fields from the request overwrite entity fields. | PUT/PATCH semantics without manual null-checking boilerplate. |
| **OpenFeign (`@EnableFeignClients`)** | Declarative HTTP client interfaces for inter-service calls. | Type-safe service contracts. No imperative RestTemplate code — Feign generates the HTTP client from the interface + annotations. |
| **Resilience4j (`circuitbreaker`, `retry`, `timelimiter`)** | Production resilience patterns configured in YAML. Circuit breaker opens at 50% failure rate; retry with exponential backoff. | Prevents cascading failures. A slow/failing downstream service triggers the breaker, failing fast instead of exhausting thread pools. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **State Machine** | Event lifecycle transitions (DRAFT→PUBLISHED→ACTIVE→COMPLETED→ARCHIVED) are validated against a `Map<EventStatus, Set<EventStatus>>` defining allowed transitions. Invalid transitions throw `InvalidStatusTransitionException`. |
| **Outbox Pattern** | Domain events are written to MongoDB `domain_events` collection as PENDING documents in the same logical operation as the database write. A separate poller publishes them asynchronously, ensuring at-least-once delivery without distributed transactions. |
| **Service Layer** | `EventService`, `AdminService`, `AuditLogService` encapsulate business logic. Controllers are thin — they validate input, delegate to services, and return DTOs. |
| **Repository Pattern** | `EventRepository`, `UserRepository` etc. abstract data access. Custom `@Query` methods provide optimized queries without exposing JPA internals. |
| **Mapper Pattern (MapStruct)** | `EventMapper`, `UserMapper` handle entity↔DTO conversion. Compile-time code generation — no reflection overhead at runtime. |

### Key Annotations & APIs

| Annotation / API | Purpose |
|-----------------|---------|
| `@EnableJpaAuditing(auditorAwareRef = "auditorAware")` | Activates JPA audit field population |
| `@Version` | Optimistic locking version counter on entities |
| `@Scheduled(fixedRateString = "${...}")` | Externalized polling interval for outbox |
| `@Transactional(readOnly = true)` | Read-only transactions skip dirty checking — faster queries |
| `ProblemDetail.forStatusAndDetail()` | RFC 7807 error response builder |
| `MongoTemplate.save(document, collectionName)` | Direct document persistence |
| `Criteria.where(...).is(...)` | MongoDB query builder |

### Concepts to Study Further

1. **Outbox Pattern vs. Change Data Capture (CDC)** — The outbox pattern writes events to a table/collection that a poller reads. CDC (Debezium) reads the database transaction log directly. Study the trade-offs: outbox is simpler but adds polling latency; CDC is real-time but requires infrastructure (Kafka Connect + Debezium).
2. **Optimistic vs. Pessimistic Locking** — `@Version` is optimistic (detect conflicts after the fact). `@Lock(LockModeType.PESSIMISTIC_WRITE)` is pessimistic (acquire lock before read). Study when each is appropriate: optimistic for low-contention reads; pessimistic for high-contention critical sections.
3. **State Machine Libraries (Spring Statemachine)** — The hand-rolled state machine map works for simple cases. For complex transitions with guards, actions, and persistence, study Spring Statemachine and its event-driven architecture.

---

## Tasks 9.1–9.4: Feedback Service (Full Implementation)

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Spring Cache Abstraction (`@Cacheable`, `@CacheEvict`)** | Declarative caching backed by Redis. Expensive queries are cached; mutations evict stale entries. | Reduces database load for frequently-accessed data (feedback stats, search results) without custom caching code. |
| **Redis Cache with Jackson Serializer** | `RedisCacheManager` configured with `GenericJackson2JsonRedisSerializer` and 10-minute TTL. | JSON-serialized cache values are debuggable (you can inspect them in Redis CLI). TTL prevents stale data without explicit invalidation. |
| **JPQL Dynamic Queries with Null Parameters** | `@Query` with `(:param IS NULL OR field = :param)` pattern allows optional filter parameters. | One query method handles any combination of filters without building queries programmatically. |
| **Bean Validation on Request DTOs** | `@Min(1) @Max(5)` on score, `@NotNull` on required fields — validated by `@Valid` on controller parameters. | Invalid input is rejected at the controller layer before reaching business logic. Structured 400 responses with field-level error messages. |
| **Testcontainers with Isolated Schema** | The feedback-service test uses its own Liquibase changelog containing only the `volunteer_feedback` table — no foreign keys to external tables. | Tests run independently without needing the full shared schema. Each service owns its test infrastructure. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Repository with Search Specification** | `VolunteerFeedbackRepository.search()` accepts 9 optional parameters — a poor man's specification pattern using JPQL null-coalescing. |
| **MapStruct Partial Update** | `FeedbackMapper.updateEntityFromRequest()` with `nullValuePropertyMappingStrategy = IGNORE` applies only provided fields. |
| **Unique Constraint as Business Rule** | `UNIQUE(event_id, volunteer_id)` enforces one feedback per volunteer per event at the database level. The service catches `DataIntegrityViolationException` and returns 409. |

### Concepts to Study Further

1. **Redis Cache Serialization Strategies** — `GenericJackson2JsonRedisSerializer` includes type information (`@class`) for polymorphic deserialization. Study the security implications (deserialization gadgets) and alternatives like `Jackson2JsonRedisSerializer` with explicit type.
2. **JPA Specifications vs. JPQL Null-Coalescing** — The current approach uses one query with null checks. JPA Specifications (`Specification<T>`) compose predicates dynamically. Study when specifications are cleaner (many optional filters, complex joins) vs. when JPQL suffices (few filters, simple conditions).

---

## Tasks 10.1–10.4: Ingestion Service (Full Implementation)

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Multipart File Upload (`@RequestParam MultipartFile`)** | Spring Boot handles multipart parsing, temp file management, and size enforcement (`spring.servlet.multipart.max-file-size`). | Built-in protection against oversized uploads (returns 413 automatically). No manual stream management needed. |
| **Apache POI (XSSFWorkbook, HSSFWorkbook, SXSSFWorkbook)** | Reads .xlsx (OOXML) and .xls (HSSF) Excel files; SXSSFWorkbook writes large files with streaming (low memory). | POI handles Excel's complex format (formulas, date cells, numeric types) correctly. SXSSFWorkbook for template generation avoids OOM on large outputs. |
| **`@ConfigurationProperties` with `@Validated` + `@NotBlank`** | `IngestionServiceProperties` requires `inputDirectory` at startup. Missing value = fast failure with clear error. | Required configuration is enforced declaratively — no `if (dir == null) throw` scattered through code. |
| **MongoDB Document Repositories** | `JobTrackingRepository`, `FileMetadataRepository`, `DomainEventRepository` — Spring Data MongoDB repos for job lifecycle, file state, and event outbox. | Same repository pattern as JPA but for documents. Automatic query derivation from method names. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Strategy (Parser Selection)** | `FileParserService` delegates to `ExcelParser` or `CsvParser` based on file extension — `switch(extension)` selects the strategy. |
| **Structured Error Reporting** | `ValidationError(rowNumber, columnName, message, rejectedValue)` provides per-cell error context. Clients know exactly which row and column failed and why. |
| **Job Tracking State Machine** | `PENDING → RUNNING → COMPLETED/FAILED/CANCELLED` — explicit status transitions with timestamp recording at each transition. |
| **Domain Event Publishing** | `DomainEventPublisher.publishVolunteersImported(...)` writes a PENDING event to MongoDB outbox. Decouples file processing from downstream reactions. |

### Concepts to Study Further

1. **Streaming Excel Parsing for Large Files** — `XSSFWorkbook` loads the entire file into memory. For files with 100K+ rows, study `XSSFReader` + SAX-based event parsing (POI's streaming API) which processes row-by-row without loading the full DOM.
2. **Idempotent Job Processing** — If the service crashes mid-import, the job is in RUNNING state. Study idempotency keys, at-least-once processing with deduplication, and how to resume from the last successfully processed row.

---

## Tasks 11.1–11.5: Notification Service (Full Implementation)

### Framework Features Used

| Feature | What It Does | Why It Matters |
|---------|-------------|----------------|
| **Spring Mail (`JavaMailSender`)** | Sends emails via SMTP. Supports `MimeMessage` for HTML content, attachments, and proper encoding. | No need for low-level JavaMail API. Spring manages connection pooling and error handling. |
| **Thymeleaf `StringTemplateResolver`** | Renders templates stored as database strings (not classpath files). Creates a fresh context per render with provided variables. | Templates are data, not code. Admin users can create/edit templates via API without redeployment. |
| **`@Async` with `ThreadPoolTaskExecutor`** | Email dispatch runs asynchronously on a dedicated thread pool (core=5, max=10, queue=100). | Sending email takes 1-5 seconds (network I/O). Async dispatch means the API responds immediately while email sends in the background. |
| **`@MockBean` for JavaMailSender** | Integration tests mock the mail sender to avoid needing a real SMTP server. Verifies dispatch logic without side effects. | Tests run fast and deterministically without network dependencies. `verify(mailSender).send(any(MimeMessage.class))` confirms the send was attempted. |
| **Awaitility** | Fluent API for asserting async outcomes: `await().atMost(5, SECONDS).untilAsserted(...)`. | Async operations complete on a different thread. Awaitility polls assertions until they pass or timeout — no `Thread.sleep()` fragility. |
| **`@ConditionalOnProperty` for Security Toggle** | `app.security.enabled=false` disables OAuth2 JWT validation in tests. Two filter chain beans coexist — property selects which one is active. | Tests don't need real JWT tokens. Production always has security enabled. Same binary, different behavior. |

### Design Patterns Applied

| Pattern | How It's Used |
|---------|--------------|
| **Retry with Exponential Backoff** | `retryBackoffMinutes: [1, 5, 30, 120, 720]` — each failure schedules the next retry further in the future. After max attempts → PERMANENTLY_FAILED. |
| **Domain Event Consumer (Polling)** | `DomainEventConsumer` polls MongoDB for unprocessed events and dispatches actions (send emails, create delivery records). Marked as processed after handling. |
| **Template Method (Rendering)** | `TemplateRenderingService` creates a Thymeleaf `Context`, sets variables, processes the template string. Same algorithm, different variables per invocation. |
| **Delivery Status Tracking** | `PENDING → QUEUED → SENT → DELIVERED/BOUNCED/FAILED → PERMANENTLY_FAILED` — each email has a MongoDB document tracking its lifecycle with timestamps at each transition. |

### Concepts to Study Further

1. **Transactional Outbox vs. Polling Consumer** — The notification service polls for domain events. Study the coupling issue: if the event-service publishes to MongoDB and the notification-service also reads from MongoDB, they share a database. A message broker (Kafka/RabbitMQ) would decouple them fully. When does the complexity of a broker pay off?
2. **Email Delivery Guarantees** — SMTP `send()` returning success means the message was accepted by the next hop, not delivered to the inbox. Study: bounce handling (DSN), delivery receipts, email reputation, and why tracking actual delivery requires webhook integrations (SES, SendGrid).
3. **Thymeleaf Security in User-Provided Templates** — Templates stored by admin users could contain malicious expressions (`${T(java.lang.Runtime).exec(...)}`). Study Thymeleaf's expression restrictions, sandboxing options, and why `StandardExpressionParser` limits what expressions can do.
