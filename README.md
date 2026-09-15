# Outreach Platform

[![CI Pipeline](https://github.com/faizalzafri/outreach-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/faizalzafri/outreach-platform/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](outreach-platform-parent/pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)](outreach-platform-parent/pom.xml)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0-6DB33F?logo=spring&logoColor=white)](outreach-platform-parent/pom.xml)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](outreach-studio/package.json)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)](outreach-studio/package.json)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](outreach-platform-parent/docker-compose.yml)
[![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb&logoColor=white)](outreach-platform-parent/docker-compose.yml)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](outreach-platform-parent/docker-compose.yml)
[![Docker Compose](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](outreach-platform-parent/docker-compose.yml)
[![Keycloak](https://img.shields.io/badge/Keycloak-26-4D4D4D?logo=keycloak&logoColor=white)](outreach-platform-parent/docker-compose.yml)

Multi-tenant SaaS platform for corporate volunteer outreach management. Manages community events, volunteer enrollment, feedback collection, notifications, analytics, and AI-powered insights — with complete data isolation between tenant organizations.

## Multi-Tenancy

The platform supports multiple organizations (NGOs, corporates, schools) on a shared deployment. Tenant isolation is enforced at every layer:
- **Database:** Shared schema with `tenant_id` column on all tables; Hibernate filters scope all queries
- **Gateway:** Extracts tenant from JWT, forwards `X-Tenant-ID` header to downstream services
- **Caching:** Redis keys prefixed with tenant ID
- **Messaging:** RabbitMQ messages carry `x-tenant-id` header

## Functional Overview

**Event Management** — Create, publish, and track outreach events through their lifecycle (Draft → Published → Active → Completed → Archived). Assign POC coordinators, enroll volunteers, mark attendance.

**Volunteer Management** — Maintain volunteer profiles with skills, department, and availability. Track participation history and leaderboard rankings. Search by skills and location.

**Bulk Data Import** — Upload Excel/CSV files to import volunteers and event data. Row-level validation with structured error reporting. Job tracking with progress.

**Feedback Collection** — Volunteers rate events (1-5) with open-ended answers. One submission per volunteer per event. Categorization, tagging, sentiment tracking. Multi-criteria search and export.

**Email Notifications** — Template-based emails triggered by events (welcome, feedback request, status changes). Delivery tracking with retry. Scheduling and preference management.

**Analytics & Reporting** — Dashboard with KPIs, trends, NPS, sentiment breakdown. Aggregate by event, city, beneficiary, or POC. Time-series with configurable granularity. Async export (PDF/CSV/Excel).

**AI Insights (Admin)** — Feedback summarization, anomaly detection, natural language queries. Feature-toggled. Provider-agnostic (OpenAI, mock).

**Administration** — User CRUD with roles (Admin/PMO/POC). Audit log. System configuration.

## Tech Stack

- Java 21, Spring Boot 3.4, Spring Cloud 2024.0
- PostgreSQL 16 (domain data), MongoDB 7 (events outbox, audit, jobs), Redis 7 (caching, rate limiting)
- Keycloak 26 / Spring Authorization Server (OAuth2/OIDC)
- Docker Compose for local development

## Services

| Service | Port | Responsibility |
|---------|------|---------------|
| discovery-service | 8761 | Eureka service registry |
| gateway-service | 7093 | API gateway, JWT validation, rate limiting |
| auth-service | 8090 | OAuth2/OIDC identity provider |
| event-service | 9004 | Event lifecycle, volunteers, beneficiaries, admin |
| feedback-service | 9001 | Feedback collection and search |
| notification-service | 9002 | Email templates, dispatch, delivery tracking |
| ingestion-service | 9003 | File upload, parsing, job tracking |
| report-service | 9005 | Analytics, aggregation, export |
| ai-service | 9006 | AI summarization, anomaly detection |

## Prerequisites

- Java 21
- Docker Desktop
- Maven (wrapper included)

## Quick Start

```bash
# Clone and enter project
cd outreach-platform/outreach-platform-parent

# Build all services
./mvnw clean package -DskipTests

# Start everything
docker compose up -d

# Verify
docker compose ps
```

Services start in dependency order (infra → platform → business). Full stack takes ~2 minutes.

## Run Without Docker

```bash
# Start infrastructure manually (PostgreSQL, MongoDB, Redis)
# Then run individual services:
./mvnw spring-boot:run -pl event-service
./mvnw spring-boot:run -pl feedback-service
# etc.
```

## Run Tests

```bash
# Unit tests (no Docker needed)
./mvnw test -DfailIfNoTests=false

# Integration tests (requires Docker for Testcontainers)
./mvnw verify
```

## User Roles

| Role | Access |
|------|--------|
| Platform Admin | Cross-tenant operations, tenant lifecycle, support access to all data |
| Tenant Admin | Manage users and roles within their tenant |
| Admin | Full access within tenant — events, reports, AI, audit |
| PMO | Events, reports, feedback, volunteers within tenant |
| POC | Assigned events, enrolled volunteers, attendance within tenant |

## API Access

All APIs require JWT authentication via the gateway at `http://localhost:7093/api/`.

```bash
# Example: list events
curl -H "Authorization: Bearer <token>" http://localhost:7093/api/events
```

## API Documentation (Swagger UI)

Each service exposes OpenAPI 3.1 docs and Swagger UI (publicly accessible in non-production):

| Service | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| event-service | http://localhost:9004/swagger-ui.html | http://localhost:9004/v3/api-docs |
| feedback-service | http://localhost:9001/swagger-ui.html | http://localhost:9001/v3/api-docs |
| ingestion-service | http://localhost:9003/swagger-ui.html | http://localhost:9003/v3/api-docs |
| notification-service | http://localhost:9002/swagger-ui.html | http://localhost:9002/v3/api-docs |
| report-service | http://localhost:9005/swagger-ui.html | http://localhost:9005/v3/api-docs |
| ai-service | http://localhost:9006/swagger-ui.html | http://localhost:9006/v3/api-docs |

Swagger UI is disabled in the `production` profile.

## Environment Variables

See `.env.example` in `outreach-platform-parent/` for all configurable values.

## Contributing

See `CONTRIBUTING.md` for development setup, coding conventions, and PR
guidelines. This project follows the `CODE_OF_CONDUCT.md`. To report a security
vulnerability, see `SECURITY.md` rather than opening a public issue.

## License

Licensed under the GNU General Public License v3.0 — see `LICENSE`.