# Gateway Service

## Purpose

API gateway built on Spring Cloud Gateway that serves as the single entry point for all client requests. Handles routing to backend microservices, JWT token validation, rate limiting, CORS enforcement, circuit breaking, and request correlation ID propagation.

## Prerequisites

- Java 21
- Redis 7.x (for rate limiting counters)
- Discovery Service running (for service resolution)
- An active identity provider — Keycloak or Spring Authorization Server (for JWT validation)

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl gateway-service
```

The server starts on **port 7093**.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `IDP_PROVIDER` | `keycloak` | Active identity provider (`keycloak` or `spring`) |
| `IDP_JWKS_URI` | Keycloak JWKS URL | JWKS endpoint for JWT signature verification |
| `REDIS_HOST` | `localhost` | Redis host for rate limiting |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | (empty) | Redis password |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | OpenTelemetry trace sampling rate |
| `OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP collector endpoint |

## Core Logic

- **Route Resolution**: Incoming requests are matched by path predicates and forwarded to the corresponding backend service via Eureka-resolved URIs (`lb://service-name`).
- **JWT Validation**: All protected routes validate the Bearer token against the configured JWKS endpoint before forwarding. Invalid or expired tokens receive HTTP 401.
- **Rate Limiting**: Redis-backed token bucket limiter at 100 requests/minute per authenticated user (burst capacity: 120). Unauthenticated requests are limited by IP.
- **Circuit Breaking**: Resilience4j circuit breaker on all downstream routes. Opens after 50% failure rate over a 10-call sliding window. Falls back to `/fallback` endpoint returning HTTP 503.
- **CORS**: Configurable allowlist (defaults to `http://localhost:4200` for Angular dev server). Supports all standard HTTP methods.
- **StripPrefix**: Removes the `/api` prefix before forwarding (e.g., `/api/feedback/search` → `/feedback/search` on feedback-service).

## Route Table

| Path Pattern | Target Service | Description |
|-------------|---------------|-------------|
| `/api/feedback/**` | feedback-service | Feedback collection and queries |
| `/api/ingestion/**` | ingestion-service | File upload and import jobs |
| `/api/events/**` | event-service | Event lifecycle management |
| `/api/reports/**` | report-service | Analytics and export |
| `/api/notifications/**` | notification-service | Email dispatch and templates |
| `/api/admin/**` | event-service | Admin operations |
| `/api/ai/**` | ai-service | AI-powered features |

## Storage

**Redis** — rate limiting counters with TTL-based expiration. No persistent database.

Rationale: Redis provides sub-millisecond reads for per-request rate limit checks, and TTL-based key expiration naturally resets counters without maintenance.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/actuator/circuitbreakers` | Circuit breaker states |

```bash
# Route a request through the gateway (requires valid JWT)
curl -H "Authorization: Bearer <token>" http://localhost:7093/api/feedback/categories

# Check gateway health
curl http://localhost:7093/actuator/health
```

## Testing

```bash
.\mvnw.cmd test -pl gateway-service
```

Tests verify route resolution, JWT validation behavior, rate limiter configuration, and circuit breaker fallback logic.
