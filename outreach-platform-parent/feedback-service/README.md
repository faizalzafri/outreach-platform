# Feedback Service

## Purpose

Handles volunteer feedback collection, validation, persistence, search, and export. Accepts feedback submissions (scores 1–5 with open-ended answers), provides filtered/paginated queries, and supports CSV/Excel export for analytics consumption.

## Prerequisites

- Java 21
- PostgreSQL 16+ (feedback records, categories)
- MongoDB 7.x (event outbox)
- Redis 7.x (query result caching)
- Docker (for Testcontainers in tests)

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl feedback-service
```

The server starts on **port 9001**.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/outreachfeedbackdb` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | `postgres` | Database username |
| `DATABASE_PASSWORD` | `postgres` | Database password |
| `MONGODB_URI` | `mongodb://localhost:27017/outreach_nosql` | MongoDB connection URI |
| `REDIS_HOST` | `localhost` | Redis host for caching |
| `REDIS_PORT` | `6379` | Redis port |
| `JWKS_URI` | `http://localhost:8090/oauth2/jwks` | JWKS endpoint for token validation |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka discovery URL |

Custom properties:

| Property | Default | Description |
|----------|---------|-------------|
| `feedback-service.max-score-value` | `5` | Maximum allowed feedback score |
| `feedback-service.min-score-value` | `1` | Minimum allowed feedback score |
| `feedback-service.default-page-size` | `20` | Default pagination size |
| `feedback-service.max-page-size` | `100` | Maximum allowed page size |

## Core Logic

- **Feedback Submission**: Validates score range (1–5), required fields, and event/volunteer existence. Persists to PostgreSQL with composite key (eventId + volunteerId).
- **Search & Filtering**: Multi-criteria search by event, employee, city, date range, score range, category, tags, and status. Results are paginated and cached in Redis (10-minute TTL).
- **Tagging & Categorization**: Feedback entries can be tagged and categorized for downstream analytics.
- **Sentiment Tracking**: Each submission carries a sentiment marker (POSITIVE, NEUTRAL, NEGATIVE) for reporting.
- **Export**: Generates CSV/Excel exports of feedback data filtered by event.
- **Cache Invalidation**: Writes (`POST`, `PUT`, `DELETE`) evict related cache entries to maintain consistency.

## Storage

| Data | Store | Rationale |
|------|-------|-----------|
| Feedback submissions, categories, tags | PostgreSQL | Composite key constraints, foreign keys to events/volunteers, complex aggregate queries for reporting |
| Domain event outbox | MongoDB | Append-only event publishing for downstream consumers |
| Query result cache | Redis | Sub-millisecond reads for repeated search queries, automatic TTL expiration |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/feedback` | Submit feedback |
| PUT | `/feedback/{eventId}/{employeeId}` | Update existing feedback |
| GET | `/feedback/{eventId}/{employeeId}` | Get feedback by composite key |
| GET | `/feedback/event/{eventId}` | List feedback for an event |
| GET | `/feedback/event/{eventId}/status` | Feedback completion stats |
| GET | `/feedback/search` | Multi-criteria search (paginated) |
| GET | `/feedback/export/{eventId}` | Export as CSV/Excel |
| GET | `/feedback/categories` | List categories |
| POST | `/feedback/{eventId}/{employeeId}/tags` | Add tags |
| DELETE | `/feedback/{eventId}/{employeeId}` | Soft-delete feedback |

```bash
# Submit feedback
curl -X POST http://localhost:9001/feedback \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"eventId":"evt-001","employeeId":"EMP123","score":4,"answer1":"Great event"}'

# Search feedback with filters
curl "http://localhost:9001/feedback/search?eventId=evt-001&minScore=3&page=0&size=20" \
  -H "Authorization: Bearer <token>"

# Get completion stats for an event
curl http://localhost:9001/feedback/event/evt-001/status \
  -H "Authorization: Bearer <token>"
```

## Testing

```bash
# Run all tests (requires Docker for Testcontainers)
.\mvnw.cmd test -pl feedback-service

# Run a specific test class
.\mvnw.cmd test -pl feedback-service "-Dtest=FeedbackSubmissionIntegrationTest"
```

Integration tests verify submission validation, search filtering, cache behavior, and export generation against real PostgreSQL and MongoDB instances via Testcontainers.
