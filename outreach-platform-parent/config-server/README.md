# Config Server

## Purpose

Centralized, profile-driven configuration management for all microservices in the Outreach Platform. Serves externalized configuration properties so that the same build artifact can be deployed to any environment without modification.

## Prerequisites

- Java 21
- (Optional) Git repository for production config storage

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl config-server
```

The server starts on **port 8888**.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `CONFIG_REPO_PATH` | `file:./config` | Path to configuration files (native mode) |
| `CONFIG_SERVER_USER` | `configadmin` | Basic auth username for config endpoints |
| `CONFIG_SERVER_PASSWORD` | `configsecret` | Basic auth password for config endpoints |
| `ENCRYPT_KEY` | `dev-encrypt-key-change-in-production` | Symmetric key for `{cipher}` value encryption |
| `EUREKA_ENABLED` | `false` | Whether to register with Eureka |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka server URL |
| `CONFIG_GIT_URI` | — | Git repo URI (when using `git` profile) |

## Core Logic

- **Native mode** (default): Reads configuration from the local filesystem under `config/` directory. Each service has a `{service-name}.yml` file.
- **Git mode** (production): Reads configuration from a remote Git repository with branch-based versioning. Activate with `spring.profiles.active=git`.
- **Encryption**: Supports `{cipher}` prefixed values in config files, encrypted/decrypted via the `/encrypt` and `/decrypt` actuator endpoints using the configured symmetric key.
- **Profiles**: Supports `development`, `staging`, `production` overlays via Spring profile resolution.

## Storage

No database. Configuration is stored on the local filesystem (native mode) or in a Git repository (git profile).

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/{application}/{profile}` | Fetch config for an application and profile |
| GET | `/{application}/{profile}/{label}` | Fetch config with branch/label |
| POST | `/encrypt` | Encrypt a plaintext value |
| POST | `/decrypt` | Decrypt a cipher value |
| GET | `/actuator/health` | Health check |

```bash
# Fetch feedback-service config for development profile
curl -u configadmin:configsecret http://localhost:8888/feedback-service/development

# Encrypt a secret value
curl -u configadmin:configsecret -X POST http://localhost:8888/encrypt -d "my-secret-value"
```

## Testing

```bash
.\mvnw.cmd test -pl config-server
```

Tests verify configuration resolution, profile handling, and encryption/decryption behavior.
