# Event Service

## Purpose

Manages the full outreach event lifecycle including event creation, status transitions, POC assignments, volunteer enrollment, beneficiary management, and attendance tracking. Also hosts admin operations (user management, audit log, system configuration).

## Prerequisites

- Java 21
- PostgreSQL 16+ (event metadata, volunteers, enrollments, beneficiaries)
- MongoDB 7.x (event outbox, audit logs)
- Redis 7.x (caching)
- Docker (for Testcontainers in tests)

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl event-service
```

The server starts on **port 9004**.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/outreachfeedbackdb` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | `postgres` | Database username |
| `DATABASE_PASSWORD` | `postgres` | Database password |
| `MONGODB_URI` | `mongodb://localhost:27017/outreach_nosql` | MongoDB connection URI |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka discovery URL |
| `JWK_SET_URI` | `http://localhost:8090/oauth2/jwks` | JWKS endpoint for token validation |

## Core Logic

- **Event Lifecycle**: Events progress through states: DRAFT → PUBLISHED → ACTIVE → COMPLETED → ARCHIVED. Also supports CANCELLED from DRAFT or PUBLISHED states.
- **POC Assignments**: Each event can have PRIMARY and SECONDARY POC (Point of Contact) users assigned for event coordination.
- **Volunteer Enrollment**: Volunteers are enrolled in events with attendance tracking (REGISTERED → ATTENDED / NOT_ATTENDED).
- **Beneficiary Management**: CRUD operations for beneficiary organizations linked to events via a many-to-many relationship.
- **Outbox Pattern**: Domain events (e.g., `VolunteersImported`, `EventStatusChanged`) are written to MongoDB outbox and polled every 5 seconds for downstream consumption.
- **Admin Operations**: User account management, role assignments, audit log queries, and system configuration — all under the `/admin/**` path.
- **Full-Text Search**: Event and volunteer search with filtering by date range, city, status, skills, and location.

## Storage

| Data | Store | Rationale |
|------|-------|-----------|
| Events, volunteers, enrollments, POC assignments, beneficiaries | PostgreSQL | Referential integrity across related entities, complex multi-table joins, ACID transactions for lifecycle state changes |
| Domain event outbox | MongoDB | Append-only writes, schema-flexible JSON, TTL indexes for automatic cleanup |
| Audit log entries | MongoDB | High write volume, flexible schema per action type, time-series queries |

## API Endpoints

**Event Management:**

| Method | Path | Description |
|--------|------|-------------|
| POST | `/events` | Create event (draft) |
| GET | `/events` | List events (filtered, paginated) |
| GET | `/events/{eventId}` | Get event details |
| PUT | `/events/{eventId}` | Update event metadata |
| PATCH | `/events/{eventId}/status` | Transition lifecycle status |
| GET | `/events/search` | Full-text search |
| GET | `/events/calendar` | Calendar view (date-range) |

**Volunteer Enrollment:**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/events/{eventId}/volunteers` | List enrolled volunteers |
| POST | `/events/{eventId}/volunteers` | Enroll volunteers |
| GET | `/events/{eventId}/attendance` | Attendance breakdown |

**Admin Operations:**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/users` | List all users |
| POST | `/admin/users` | Create user account |
| GET | `/admin/audit-log` | Query audit log |

```bash
# Create a new event
curl -X POST http://localhost:9004/events \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"eventName":"Community Cleanup","city":"Bangalore","eventDate":"2025-03-15"}'

# Transition event status
curl -X PATCH http://localhost:9004/events/{eventId}/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"status":"PUBLISHED"}'

# Search events
curl "http://localhost:9004/events/search?city=Bangalore&status=ACTIVE" \
  -H "Authorization: Bearer <token>"
```

## Testing

```bash
# Run all tests (requires Docker for Testcontainers)
.\mvnw.cmd test -pl event-service

# Run a specific test class
.\mvnw.cmd test -pl event-service "-Dtest=EventLifecycleIntegrationTest"
```

Integration tests use Testcontainers with real PostgreSQL and MongoDB instances to verify event lifecycle transitions, enrollment operations, and outbox event publishing.
