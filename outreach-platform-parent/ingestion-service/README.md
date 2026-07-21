# Ingestion Service

## Purpose

Handles file upload (Excel/CSV), parsing, validation, and bulk data import into the platform. Publishes domain events for downstream processing by other services (Event Service, Notification Service, Report Service). Provides job tracking so administrators can monitor import progress and review errors.

## Prerequisites

- Java 21
- PostgreSQL 16+ (volunteer records, event summaries)
- MongoDB 7.x (job tracking documents, file processing metadata)
- Docker (for Testcontainers in tests)

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl ingestion-service
```

The server starts on **port 9003**.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/outreachfeedbackdb` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | `postgres` | Database username |
| `DATABASE_PASSWORD` | `postgres` | Database password |
| `MONGODB_URI` | `mongodb://localhost:27017/outreach_nosql` | MongoDB connection URI |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka discovery URL |
| `JWT_JWK_SET_URI` | `http://localhost:8090/oauth2/jwks` | JWKS endpoint for token validation |
| `INGESTION_INPUT_DIR` | (empty) | Optional filesystem directory for file pickup |

Custom properties:

| Property | Default | Description |
|----------|---------|-------------|
| `ingestion-service.max-file-size-mb` | `25` | Maximum upload file size |
| `ingestion-service.allowed-extensions` | `.xlsx, .xls, .csv` | Accepted file types |
| `ingestion-service.max-concurrent-jobs` | `5` | Maximum parallel import jobs |

## Core Logic

- **File Upload**: Accepts multipart file uploads (single or bulk). Validates file type, size (max 25MB), and basic structure before processing.
- **Excel/CSV Parsing**: Uses Apache POI for Excel files and standard CSV parsing. Validates each row against expected column schema (employee ID, name, email, event code, etc.).
- **Dry-Run Validation**: `/ingestion/validate` endpoint performs full parsing and validation without persisting data — useful for previewing errors before committing an import.
- **Job Tracking**: Each import creates a job document in MongoDB with status progression (QUEUED → PROCESSING → COMPLETED / FAILED), percentage progress, row counts, and error details.
- **Domain Events Published**:
  - `VolunteersImported` — consumed by Event Service (enrollment) and Notification Service (welcome emails)
  - `EventSummaryImported` — consumed by Event Service (event metadata creation)
  - `ImportJobCompleted` — consumed by Report Service (analytics refresh)
- **Error Reporting**: Invalid rows are captured with row number, column, and validation error. Retrievable via the job errors endpoint.
- **Rate Limiting**: Inbound uploads are rate-limited (50 requests/second) to protect against abuse.

## Storage

| Data | Store | Rationale |
|------|-------|-----------|
| Parsed volunteer records, event summaries | PostgreSQL | Referential integrity, joins with event enrollment tables, ACID transactions for bulk inserts |
| Job tracking documents | MongoDB | Flexible status/metadata schema, progress updates, no referential needs, TTL-based cleanup |
| File processing metadata | MongoDB | Variable metadata per file type (column mappings, parsing config), processing pipeline state |
| Domain event outbox | MongoDB | Append-only event publishing for downstream consumers |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/ingestion/upload` | Upload a single file |
| POST | `/ingestion/upload/bulk` | Upload multiple files |
| GET | `/ingestion/jobs` | List import jobs (paginated) |
| GET | `/ingestion/jobs/{jobId}` | Get job status and progress |
| DELETE | `/ingestion/jobs/{jobId}` | Cancel a pending/running job |
| GET | `/ingestion/jobs/{jobId}/errors` | Get detailed error rows |
| GET | `/ingestion/templates` | Download import template files |
| POST | `/ingestion/validate` | Dry-run validation |

```bash
# Upload an Excel file
curl -X POST http://localhost:9003/ingestion/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@volunteers.xlsx"

# Check job status
curl http://localhost:9003/ingestion/jobs/{jobId} \
  -H "Authorization: Bearer <token>"

# Dry-run validation without import
curl -X POST http://localhost:9003/ingestion/validate \
  -H "Authorization: Bearer <token>" \
  -F "file=@volunteers.xlsx"
```

## Testing

```bash
# Run all tests (requires Docker for Testcontainers)
.\mvnw.cmd test -pl ingestion-service

# Run a specific test class
.\mvnw.cmd test -pl ingestion-service "-Dtest=ExcelParsingIntegrationTest"
```

Integration tests verify file parsing, row validation, job tracking state transitions, domain event publishing, and error capture against real PostgreSQL and MongoDB via Testcontainers. Sample Excel/CSV fixtures are included in `src/test/resources/`.
