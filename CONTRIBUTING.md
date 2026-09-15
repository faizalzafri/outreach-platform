# Contributing to Outreach Platform

Thanks for considering a contribution. This repo hosts two independently-versioned projects:

- `outreach-platform-parent/` — Java 21 / Spring Boot 3.4 microservices backend
- `outreach-studio/` — React 19 + TypeScript admin frontend

Start with the root `README.md` for the service map, ports, and quick start, and
`CLAUDE.md` for the fuller architecture/conventions reference (multi-tenancy model,
package layout, testing strategy, etc.) — this document covers the mechanics of
making a change, not the architecture itself.

## Before you start

- **Bug fix or small change:** open a PR directly, no need to file an issue first.
- **New feature or anything touching shared infrastructure** (common-lib, the
  multi-tenancy model, auth, the gateway filter chain): open an issue first to agree
  on the approach before investing time in an implementation. For anything
  substantial enough to need its own design doc, see `docs/specs/` for the format
  this repo uses (requirements.md / design.md / tasks.md) and `docs/adr/` for
  recording a decision with real trade-offs on both sides.
- Check `docs/specs/*/tasks.md` for in-flight work before starting something that
  might overlap.

## Development setup

```bash
# Backend
cd outreach-platform-parent
./mvnw clean package -DskipTests
docker compose up -d          # infra + all services
docker compose ps              # confirm everything is healthy

# Frontend
cd outreach-studio
npm ci
npm run dev                    # http://localhost:5173
```

See the README's Prerequisites section for required tooling (Java 21, Docker
Desktop, Node).

## Making a change

**Backend (`outreach-platform-parent/`)**

- Constructor injection via `jakarta.inject.Inject`, never `@Autowired` field
  injection — an ArchUnit rule enforces this.
- New tenant-scoped entities/repositories must follow the multi-tenancy pattern in
  `CLAUDE.md` (extend `TenantAwareBaseEntity`, add a `findByIdAndTenantId` method
  rather than calling bare `findById`). This is the single most common source of a
  real security bug in this codebase — read that section before adding one.
- Don't add a dependency version to a child POM if the parent already manages it.
- Bean Validation on every request DTO; PostgreSQL JSONB/native enums go through
  Hypersistence Utils, not VARCHAR workarounds; inter-service calls use OpenFeign.
- Never log a `@PiiField`-annotated field, even for debugging — CI greps for this
  and fails the build.

**Frontend (`outreach-studio/`)**

- File-based routing (TanStack Router), TanStack Query for server state, Zustand
  for client/UI state.
- Colors come from `src/styles/theme.css` custom properties — never hardcode a
  color in component CSS.
- Tests live in `__tests__/` next to what they cover (Vitest + React Testing
  Library + MSW + jest-axe).

## Tests

Every change should carry test coverage proportional to its risk. This repo treats
integration tests against real Postgres/Mongo/RabbitMQ/Redis via Testcontainers as
the primary backend test strategy — unit tests should minimize mocking and only
mock true external boundaries (HTTP, SMTP, filesystem).

```bash
# Backend — unit tests (no Docker needed)
./mvnw test -DfailIfNoTests=false

# Backend — unit + integration tests (Docker required)
./mvnw verify

# Single module / single class
./mvnw verify -pl event-service -Dtest=EventServiceTest

# Frontend
npm run test -- --run
npm run lint
```

A PR that adds an integration test class must confirm it actually executes (check
the `mvn verify` output for the class name under Tests run) rather than assuming
Maven picked it up — this repo has direct history of test classes existing on disk
but never being wired into the build.

## Commit and PR conventions

- Commit messages: short imperative summary line, describing the change itself
  (not a task/ticket number — this repo doesn't reference spec task IDs in commits).
- Keep PRs focused. A bug fix doesn't need an accompanying refactor.
- If your PR checks off a box in a `docs/specs/*/tasks.md`, include (or link) the
  test/CI evidence proving it's done in the same PR — a checked box without
  evidence isn't treated as settled here.
- CI must pass: unit tests, integration tests, lint, and the PII-logging grep.

## Reporting bugs

Open an issue with: what you expected, what happened instead, and enough to
reproduce it (which service, request/response if applicable, relevant logs with
any PII/secrets redacted).

## Reporting security issues

Do **not** open a public issue for a security vulnerability — see `SECURITY.md`.

## Code of conduct

This project follows the guidelines in `CODE_OF_CONDUCT.md`.

## License

By contributing, you agree that your contributions will be licensed under this
project's license (GNU GPL v3 — see `LICENSE`).
