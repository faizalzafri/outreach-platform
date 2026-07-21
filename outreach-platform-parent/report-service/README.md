# Report Service

## Purpose

Provides analytics aggregation, dashboard data, time-series queries, and report export (PDF, CSV, Excel) for the Outreach Platform. Operates as a read-only consumer of data written by other services, with pre-computed analytics snapshots stored in MongoDB for fast dashboard rendering.

## Prerequisites

- Java 21
- PostgreSQL 16+ (read-only access to feedback, event, volunteer data)
- MongoDB 7.x (analytics snapshots, export job tracking)
- Redis 7.x (query result caching)
- Docker (for Testcontainers in tests)

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl report-service
```

The server starts on **port 9005**.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/outreachfeedbackdb` | PostgreSQL connection URL (read-only) |
| `DATABASE_USERNAME` | `postgres` | Database username |
| `DATABASE_PASSWORD` | `postgres` | Database password |
| `MONGODB_URI` | `mongodb://localhost:27017/outreach_nosql` | MongoDB connection URI |
| `REDIS_HOST` | `localhost` | Redis host for caching |
| `REDIS_PORT` | `6379` | Redis port |
| `JWK_SET_URI` | `http://localhost:8090/oauth2/jwks` | JWKS endpoint for token validation |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka discovery URL |

Custom properties:

| Property | Default | Description |
|----------|---------|-------------|
| `report-service.default-page-size` | `20` | Default pagination size |
| `report-service.max-page-size` | `100` | Maximum page size |
| `report-service.cache-ttl-minutes` | `10` | Redis cache TTL for query results |
| `report-service.snapshot-retention-days` | `365` | MongoDB snapshot retention period |

## Core Logic

- **Aggregation Queries**: Computes feedback score averages, counts, and distributions grouped by event, beneficiary, city, or POC. Uses PostgreSQL aggregate functions for real-time queries.
- **Time-Series Analytics**: Supports day/week/month/quarter granularity for trend visualization. Dashboard trend data is cached in Redis.
- **Pre-Computed Snapshots**: Periodic background jobs compute dashboard KPIs and store results in MongoDB. Dashboard endpoints serve from snapshots for sub-second response times.
- **Export Generation**: Asynchronous export jobs produce PDF, CSV, or Excel files. Returns 202 with a job ID; clients poll for completion and download.
- **Scheduled Reports**: Cron-based report generation with email delivery to configured recipients.
- **Comparison & Heatmap**: Compare performance across events/periods and generate geographic participation heatmaps.
- **Read-Only Design**: This service never writes to PostgreSQL. Liquibase is disabled. Schema is managed by the Event Service and Feedback Service.

## Storage

| Data | Store | Rationale |
|------|-------|-----------|
| Source data (feedback, events, volunteers) | PostgreSQL (read-only) | Complex aggregate queries with JOINs across multiple tables, SQL window functions for time-series |
| Analytics snapshots | MongoDB | Schema-flexible aggregation results, time-bucketed documents, no referential integrity needed |
| Export job tracking | MongoDB | Flexible job metadata, status progression, binary file references |
| Query result cache | Redis | Fast cache reads for frequently-accessed dashboard data, TTL-based expiration |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/reports/by-event` | Aggregate scores by event |
| GET | `/reports/by-beneficiary` | Aggregate by beneficiary |
| GET | `/reports/by-city` | Aggregate by city |
| GET | `/reports/by-poc` | Aggregate by POC |
| GET | `/reports/dashboard` | Dashboard summary stats |
| GET | `/reports/dashboard/trends` | Time-series trend data |
| GET | `/reports/dashboard/kpis` | Key performance indicators |
| GET | `/reports/time-series` | Custom time-series query |
| GET | `/reports/comparison` | Compare events/periods |
| POST | `/reports/export` | Generate export (PDF/CSV/Excel) |
| GET | `/reports/export/{jobId}` | Get export status/download |
| GET | `/reports/scheduled` | List scheduled reports |
| POST | `/reports/scheduled` | Create scheduled report |

```bash
# Get dashboard summary
curl http://localhost:9005/reports/dashboard \
  -H "Authorization: Bearer <token>"

# Get time-series trends (monthly granularity)
curl "http://localhost:9005/reports/dashboard/trends?dateFrom=2024-01-01&dateTo=2024-12-31&granularity=month" \
  -H "Authorization: Bearer <token>"

# Request a CSV export
curl -X POST http://localhost:9005/reports/export \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"reportType":"BY_EVENT","exportFormat":"CSV","dateFrom":"2024-01-01","dateTo":"2024-12-31"}'
```

## Testing

```bash
# Run all tests (requires Docker for Testcontainers)
.\mvnw.cmd test -pl report-service

# Run a specific test class
.\mvnw.cmd test -pl report-service "-Dtest=AggregationIntegrationTest"
```

Integration tests verify aggregation accuracy, time-series computation, export generation, and cache behavior against real PostgreSQL and MongoDB via Testcontainers. Test fixtures seed known data to assert deterministic aggregation results.
