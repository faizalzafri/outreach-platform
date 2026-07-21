# AI Service

## Purpose

AI abstraction layer providing admin-only intelligent features: feedback summarization, anomaly detection in scores, and natural language data queries. Built on Spring AI with a pluggable provider architecture that supports OpenAI, Amazon Bedrock, Ollama, or a mock provider for development.

## Prerequisites

- Java 21
- MongoDB 7.x (job results, prompt cache)
- Docker (for Testcontainers in tests)
- (Optional) An AI provider API key (OpenAI, Bedrock, etc.)

## Running Locally

```bash
cd outreach-platform-parent
.\mvnw.cmd spring-boot:run -pl ai-service
```

The server starts on **port 9006**. By default, all AI features are **disabled** via feature toggles. Enable them individually as needed.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGODB_URI` | `mongodb://localhost:27017/outreach_nosql` | MongoDB connection URI |
| `JWKS_URI` | `http://localhost:8090/oauth2/jwks` | JWKS endpoint for token validation |
| `EUREKA_URI` | `http://localhost:8761/eureka` | Eureka discovery URL |
| `AI_PROVIDER` | `openai` | Active AI provider (`openai`, `bedrock`, `ollama`, `mock`) |
| `AI_API_KEY` | (empty) | AI provider API key |
| `AI_MODEL` | `gpt-4o-mini` | AI model identifier |
| `AI_MAX_TOKENS` | `4096` | Max tokens per request |
| `AI_TEMPERATURE` | `0.7` | Sampling temperature |
| `AI_JOB_TTL_HOURS` | `24` | How long job results are retained in MongoDB |
| `AI_SUMMARIZE_ENABLED` | `false` | Enable summarization feature |
| `AI_ANOMALIES_ENABLED` | `false` | Enable anomaly detection feature |
| `AI_QUERY_ENABLED` | `false` | Enable natural language query feature |

## Core Logic

- **Async Job Pattern**: All AI endpoints are asynchronous. They return HTTP 202 with a `jobId`. Clients poll `/ai/jobs/{jobId}` for results.
- **Summarization**: Aggregates feedback text for an event and produces a concise AI-generated summary highlighting key themes, sentiment distribution, and actionable insights.
- **Anomaly Detection**: Analyzes score distributions to identify statistical outliers, unusual patterns, or sudden shifts in feedback quality.
- **Natural Language Queries**: Translates plain-English questions about feedback data into structured queries, executes them, and returns human-readable answers.
- **Feature Toggles**: Each feature is independently toggleable via `platform.ai.features.<name>.enabled`. Disabled features return HTTP 404.
- **Result Caching**: AI job results are stored in MongoDB with a configurable TTL (default 24 hours) to avoid redundant provider calls.
- **Resilience**: Circuit breaker wraps all AI provider calls with a 30-second timeout, 50% failure threshold, and 60-second recovery window.

## Storage

| Data | Store | Rationale |
|------|-------|-----------|
| AI job results and cached responses | MongoDB | Schema-flexible result documents (summaries, anomaly reports, query answers vary in structure), TTL-based automatic cleanup |
| Prompt templates | Filesystem (`resources/prompts/`) | Version-controlled with the service, no runtime persistence needed |

This service does **not** use PostgreSQL. JPA, DataSource, and Liquibase auto-configuration are explicitly excluded.

## AI Extension Points

The service is designed for easy extension when adding new AI capabilities:

### 1. Adding a New AI Provider

Implement the `AiService` interface:

```java
public interface AiService {
    CompletableFuture<AiJobResult> summarize(SummarizeRequest request);
    CompletableFuture<AiJobResult> detectAnomalies(AnomalyRequest request);
    CompletableFuture<AiJobResult> query(QueryRequest request);
}
```

Existing implementations:
- `OpenAiServiceImpl` — production provider using Spring AI's OpenAI integration
- `MockAiService` — returns canned responses for local development and testing

Register your implementation as a Spring bean activated by the `platform.ai.provider` property value.

### 2. Adding a New AI Feature

1. Add a feature toggle in `application.yml` under `platform.ai.features.<name>.enabled`
2. Create request/response models in the `model/` package
3. Add the operation method to the `AiService` interface
4. Implement in both `OpenAiServiceImpl` and `MockAiService`
5. Add the endpoint in `AiController` with a feature toggle guard
6. Create a prompt template in `src/main/resources/prompts/`

### 3. Prompt Template Customization

Prompt templates are stored in `src/main/resources/prompts/` as plain text files. They use placeholder variables that are substituted at runtime. To customize AI behavior, edit or add templates without code changes.

### 4. Configuration Properties

The `AiServiceProperties` record (bound to `platform.ai.*`) provides type-safe access to all configuration. Extend it by adding new fields to the record and corresponding YAML entries.

## API Endpoints

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| POST | `/ai/summarize` | Summarize event feedback | ADMIN |
| POST | `/ai/anomalies` | Detect score anomalies | ADMIN |
| POST | `/ai/query` | Natural language data query | ADMIN |
| GET | `/ai/jobs/{jobId}` | Poll for job result | ADMIN |
| GET | `/ai/status` | Service health and capabilities | ADMIN |

```bash
# Request a feedback summary (returns 202 + jobId)
curl -X POST http://localhost:9006/ai/summarize \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"eventId":"evt-001"}'

# Poll for result
curl http://localhost:9006/ai/jobs/{jobId} \
  -H "Authorization: Bearer <token>"

# Check which AI features are enabled
curl http://localhost:9006/ai/status \
  -H "Authorization: Bearer <token>"
```

## Testing

```bash
# Run all tests (requires Docker for Testcontainers)
.\mvnw.cmd test -pl ai-service

# Run a specific test class
.\mvnw.cmd test -pl ai-service "-Dtest=AiControllerIntegrationTest"
```

Integration tests use the `MockAiService` implementation and Testcontainers with MongoDB to verify job creation, polling, TTL expiration, feature toggle behavior, and error handling without making real AI provider calls.
