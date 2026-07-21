# Discovery Service

## Purpose

Service registry using Netflix Eureka that enables dynamic service-to-service communication. All microservices register themselves on startup and discover other services through this registry, eliminating hardcoded host/port references.

## Prerequisites

- Java 21

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl discovery-service
```

The server starts on **port 8761**. The Eureka dashboard is accessible at `http://localhost:8761/`.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `EUREKA_USER` | `eurekaadmin` | Basic auth username for dashboard and API |
| `EUREKA_PASSWORD` | `eurekasecret` | Basic auth password |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | OpenTelemetry trace sampling rate |
| `OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP collector endpoint |

## Core Logic

- **Service Registration**: Microservices register on startup with their hostname, port, and health status. Eureka assigns an instance ID and tracks heartbeats.
- **Service Discovery**: Clients query Eureka for available instances of a target service. The gateway and OpenFeign clients use this to resolve `lb://service-name` URIs.
- **Self-Preservation**: When network partitions occur, Eureka avoids mass-evicting instances by entering self-preservation mode (threshold: 85% renewal rate).
- **Eviction**: In development, unhealthy instances are evicted every 5 seconds. In production, this is tuned for stability.
- **Standalone Mode**: Runs as a single instance that does not register with itself (`register-with-eureka: false`).

## Storage

No database. Service registry state is held in-memory. Instances re-register on startup if the discovery service restarts.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Eureka dashboard (HTML) |
| GET | `/eureka/apps` | List all registered applications |
| GET | `/eureka/apps/{appId}` | Get instances of a specific service |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |

```bash
# List all registered services
curl -u eurekaadmin:eurekasecret http://localhost:8761/eureka/apps -H "Accept: application/json"

# Check health
curl http://localhost:8761/actuator/health
```

## Testing

```bash
.\mvnw.cmd test -pl discovery-service
```

Tests verify Eureka server startup, self-preservation configuration, and actuator endpoint availability.
