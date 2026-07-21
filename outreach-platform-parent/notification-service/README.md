# Notification Service

## Purpose

Manages email composition, template rendering, SMTP delivery, scheduling, and delivery status tracking. Consumes domain events from other services to trigger automated notifications (e.g., feedback request emails after volunteer import, event lifecycle alerts).

## Prerequisites

- Java 21
- PostgreSQL 16+ (notification templates, schedules)
- MongoDB 7.x (delivery tracking logs, audit entries)
- SMTP server (or local MailHog/Mailpit for development)
- Docker (for Testcontainers in tests)

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl notification-service
```

The server starts on **port 9002**.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/outreachfeedbackdb` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | `postgres` | Database username |
| `DATABASE_PASSWORD` | `postgres` | Database password |
| `MONGODB_URI` | `mongodb://localhost:27017/outreach_nosql` | MongoDB connection URI |
| `SMTP_HOST` | `localhost` | SMTP server host |
| `SMTP_PORT` | `587` | SMTP server port |
| `SMTP_USERNAME` | (empty) | SMTP username |
| `SMTP_PASSWORD` | (empty) | SMTP password |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka discovery URL |

Custom properties:

| Property | Default | Description |
|----------|---------|-------------|
| `notification-service.from-address` | `noreply@outreach.com` | Sender email address |
| `notification-service.from-name` | `Outreach Platform` | Sender display name |
| `notification-service.batch-size` | `50` | Emails per batch dispatch |
| `notification-service.max-retry-attempts` | `5` | Max delivery retries per email |
| `notification-service.retry-backoff-minutes` | `1,5,30,120,720` | Exponential backoff schedule |

## Core Logic

- **Template Rendering**: Thymeleaf-based HTML email templates with variable substitution. Templates are stored in PostgreSQL with versioning and a JSON schema describing expected variables.
- **Batch Dispatch**: Emails are sent in configurable batches (default 50) with async processing to avoid blocking.
- **Delivery Tracking**: Each email attempt is logged to MongoDB with status (PENDING → SENT → DELIVERED / FAILED / BOUNCED) and error metadata.
- **Scheduling**: Supports cron-based and event-lifecycle-triggered notification schedules. Scheduled notifications execute at the specified time or upon event state transitions.
- **Retry with Backoff**: Failed emails are retried up to 5 times with exponential backoff (1min, 5min, 30min, 2hr, 12hr).
- **Domain Event Consumption**: Listens for `SendFeedbackEmails`, `EventStatusChanged`, and `VolunteersImported` events to trigger automated email workflows.
- **Preferences**: Per-volunteer notification preferences (opt-in/opt-out by category).

## Storage

| Data | Store | Rationale |
|------|-------|-----------|
| Notification templates, schedules, preferences | PostgreSQL | Referential integrity (FK to events), versioned templates, schedule management with cron expressions |
| Email delivery logs | MongoDB | Append-only delivery attempt records, flexible error metadata per attempt, no referential needs |
| Audit entries | MongoDB | High write volume, flexible schema, time-series queries |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/notifications/status/{eventId}` | Delivery status for an event |
| POST | `/notifications/retry/{eventId}` | Retry failed emails |
| POST | `/notifications/send` | Send ad-hoc notification |
| GET | `/notifications/templates` | List templates |
| POST | `/notifications/templates` | Create template |
| PUT | `/notifications/templates/{id}` | Update template |
| GET | `/notifications/templates/{id}/preview` | Preview rendered template |
| GET | `/notifications/schedule` | List scheduled notifications |
| POST | `/notifications/schedule` | Schedule a notification |
| GET | `/notifications/history` | Delivery history (paginated) |
| GET | `/notifications/analytics` | Delivery rate analytics |

```bash
# Check delivery status for an event
curl http://localhost:9002/notifications/status/evt-001 \
  -H "Authorization: Bearer <token>"

# Retry failed emails
curl -X POST http://localhost:9002/notifications/retry/evt-001 \
  -H "Authorization: Bearer <token>"

# Preview a template
curl http://localhost:9002/notifications/templates/{templateId}/preview \
  -H "Authorization: Bearer <token>"
```

## Testing

```bash
# Run all tests (requires Docker for Testcontainers)
.\mvnw.cmd test -pl notification-service

# Run a specific test class
.\mvnw.cmd test -pl notification-service "-Dtest=EmailDispatchIntegrationTest"
```

Integration tests verify template rendering, batch dispatch logic, retry behavior, and delivery status tracking against real PostgreSQL and MongoDB via Testcontainers. SMTP is mocked using GreenMail or a test mail server container.
