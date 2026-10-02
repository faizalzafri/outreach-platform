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
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?logo=rabbitmq&logoColor=white)](outreach-platform-parent/docker-compose.yml)

Multi-tenant SaaS platform for corporate volunteer outreach management. Manages community events, volunteer enrollment, feedback collection, notifications, analytics, and AI-powered insights — with complete data isolation between tenant organizations.

## Multi-Tenancy

The platform supports multiple organizations (NGOs, corporates, schools) on a shared deployment. Tenant isolation is enforced at every layer:
- **Database:** Shared schema with `tenant_id` column on all tables; Hibernate filters scope all queries
- **Gateway:** Extracts tenant from JWT, forwards `X-Tenant-ID` header to downstream services, and enforces organization status (suspended = read-only, deactivated = blocked)
- **Caching:** Redis keys prefixed with tenant ID
- **Messaging:** RabbitMQ messages carry `x-tenant-id` header

## Functional Overview

**Event Management** — Create, publish, and track outreach events through their lifecycle (Draft → Published → Active → Completed → Archived, or Cancelled). Assign POCs, share events with teams, link beneficiaries, enroll volunteers, record attendance. List and calendar views; cancelled and archived events are locked.

**Volunteer Management** — Maintain volunteer profiles with skills, department, and availability. Track participation history and leaderboard rankings. Search by skills and location.

**Bulk Data Import** — Upload Excel/CSV files to import volunteers and event data. Row-level validation with structured error reporting. Job tracking with progress.

**Feedback Collection** — Volunteers rate events (1-5) with open-ended answers. One submission per volunteer per event, only once the event is active. Fixed categories, tags, sentiment tracking. Multi-criteria search (including by tag) and CSV export per event.

**Email Notifications** — Template-based emails triggered by events (feedback request, status changes) and account emails (invitations, password resets, one-time codes). Delivery tracking with retry. Channels are pluggable (email today).

**Analytics & Reporting** — Dashboard with KPIs, trends and score distribution. Insights: attendance and feedback rates, NPS, sentiment, activity by city, comparisons. Aggregate by event, city, beneficiary, or POC. Export (PDF/CSV/Excel) on demand or emailed on a schedule.

**AI Insights (Admin)** — Feedback summarization, anomaly detection, natural language queries. Feature-toggled. Provider-agnostic (OpenAI, mock).

**Identity & Administration** — Users are invited by email and set their own password; forgot/reset password by link. Password policy and one-time codes (sign-in, reset, password change) configurable per organization. Platform admins create and manage organizations from the UI. Profile settings, audit log.

## Tech Stack

- Java 21, Spring Boot 3.4, Spring Cloud 2024.0
- PostgreSQL 16 (domain data), MongoDB 7 (events outbox, audit, jobs), Redis 7 (caching, rate limiting), RabbitMQ (domain events)
- Spring Authorization Server (OAuth2/OIDC, in auth-service)
- React 19 + TypeScript frontend (`outreach-studio`)
- Docker Compose for local development, MailHog to catch emails

## Services

| Service | Port | Responsibility |
|---------|------|---------------|
| discovery-service | 8761 | Eureka service registry |
| gateway-service | 7093 | API gateway, JWT validation, rate limiting |
| auth-service | 8090 | OAuth2/OIDC identity provider; accounts, invitations, passwords, one-time codes, organizations |
| event-service | 9004 | Event lifecycle, volunteers, beneficiaries, teams, user directory, audit log |
| feedback-service | 9001 | Feedback collection and search |
| notification-service | 9002 | Email templates, dispatch, delivery tracking |
| ingestion-service | 9003 | File upload, parsing, job tracking |
| report-service | 9005 | Analytics, aggregation, export |
| ai-service | 9006 | AI summarization, anomaly detection |
| outreach-studio | 5173 | Web app (Vite dev server) |
| MailHog | 8025 | Catches all outgoing email in development |

## Prerequisites

- Java 21
- Node.js 20+ (for the web app)
- Docker Desktop
- Maven (wrapper included)

## Quick Start

```bash
# Backend: images are built from source by Docker
cd outreach-platform/outreach-platform-parent
docker compose up -d --build
docker compose ps

# Web app
cd ../outreach-studio
npm ci
npm run dev        # http://localhost:5173
```

Services start in dependency order (infra → platform → business). Full stack takes a few minutes on the first build.

On first start, auth-service invites the platform admin (`PLATFORM_ADMIN_EMAIL`, `platform.admin@outreach-platform.com` in Compose). Open MailHog at http://localhost:8025, follow the activation link to set a password, then sign in and create organizations from **Organizations**.

## Run Without Docker

```bash
# Start infrastructure manually (PostgreSQL, MongoDB, Redis, RabbitMQ, MailHog)
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
| POC | Events assigned to them or shared with their team: volunteers, attendance, feedback |

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