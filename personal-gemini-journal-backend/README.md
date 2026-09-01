# Personal Gemini Journal Backend

Spring Boot 3 REST API for authenticated journaling, private vector retrieval, and automated accountability. Java interfaces separate authentication, AI, embedding, and persistence concerns so the same HTTP application runs locally or on Google Cloud.

## Technology

- Java 17 and Spring Boot 3.4.8.
- Spring Web, Security, OAuth2 Resource Server, Validation, JDBC, Actuator, and scheduling/async support.
- PostgreSQL 16, Flyway, and pgvector for local persistence/RAG.
- Keycloak OIDC JWT validation locally.
- Ollama chat and embedding REST APIs locally.
- Firebase Admin, Cloud Firestore, Secret Manager, and Gemini REST APIs in cloud mode.
- JUnit 5, Mockito, Spring Security Test, and Testcontainers.

## Package responsibilities

| Package | Responsibility |
|---|---|
| `security` | Local OIDC/cloud Firebase authentication, principal mapping, CORS, per-user quotas |
| `account` | UID-scoped application-data and cloud identity deletion |
| `chat` | Journal/RAG orchestration and accountability dispatch/outbox worker |
| `gemini` | Provider-neutral AI ports plus Ollama and Gemini adapters |
| `journal` | DTOs, models, persistence port, JDBC and Firestore adapters |
| `config` | Profile-specific clients, typed properties, executors/scheduling |
| `common` | Sanitized RFC Problem Details exception mapping |

## Profiles

`local` is the default profile. It requires PostgreSQL/pgvector, Keycloak, and Ollama. `cloud` disables JDBC/Flyway auto-configuration and enables Firebase, Firestore, Secret Manager, and Gemini beans.

| Port/interface | Local adapter | Cloud adapter |
|---|---|---|
| `JournalRepository` | `JdbcJournalRepository` | `FirestoreJournalRepository` |
| `GenerativeAiService` | `OllamaAiService` | `GeminiService` |
| `EmbeddingService` | `OllamaAiService` | `GeminiEmbeddingService` |
| `AccountabilityDispatcher` | Transactional JDBC outbox | `AccountabilityService` with `@Async` |

## Authentication and ownership

All routes except `/actuator/health/**` require a bearer token. Local JWT validation checks signature against the configured JWK set, issuer, expiration/not-before, required audience, and a bounded subject. The subject becomes `FirebasePrincipal.uid()` only after validation. Cloud mode calls `FirebaseAuth.verifyIdToken(token, true)`.

Controllers never accept a UID. Repository calls always receive the UID from the authenticated principal.

Local JDBC operations set transaction-local `app.current_user_id`, include explicit UID predicates, and run against forced RLS policies. The Docker database bootstrap creates a non-superuser app role. Firestore operations use only:

```text
users/{uid}/journal_entries/{entryId}
users/{uid}/action_items/{actionItemId}
```

## Persistence-first AI processing, RAG, and accountability

`POST /api/journal/entry` validates and saves journal text before invoking an AI provider. It returns `202 Accepted` with `processingStatus=PENDING`. Local PostgreSQL creates the entry and outbox job atomically; the worker later adds reflection and embedding, then produces `PROPOSED` goal suggestions. Terminal provider failures set a safe `FAILED` state while preserving the original journal, and an owned retry endpoint resets the existing durable job.

Local vectors use `vector(768)` and a cosine HNSW index. RAG performs `ORDER BY embedding <=> :queryVector` only after UID filtering and excludes entries without completed embeddings. Cloud mode performs bounded in-memory cosine ranking over documents already loaded from the UID path.

The worker claims jobs with `FOR UPDATE SKIP LOCKED`, reclaims stale jobs, applies exponential retries, and deduplicates proposals by owner/source entry/goal. Goal extraction is optional: its failure cannot discard a successful reflection. Cloud mode currently uses a best-effort `@Async` adapter and must move to Cloud Tasks or Pub/Sub before multi-instance production use.

AI suggestions start as `PROPOSED`. Only the user can PATCH them to `PENDING`; dismissal uses DELETE. This prevents autonomous model output from silently changing the user's accountability plan.

## Availability, rate limits, and deletion

The authenticated filter applies fixed-window quotas keyed by verified UID: 30 journal writes/hour, 20 RAG calls/hour, and 120 other API requests/minute by default. Responses include standard limit metadata and return `429` with `Retry-After`. These values are configurable through `JOURNAL_WRITES_PER_HOUR`, `RAG_QUERIES_PER_HOUR`, and `API_REQUESTS_PER_MINUTE`.

`DELETE /api/account` accepts no UID. Local mode transactionally deletes action items, journal entries, and cascaded jobs; Keycloak remains the external identity authority. Cloud mode deletes isolated Firestore documents first and then deletes the Firebase identity. Data-first ordering avoids creating undeletable records if identity deletion fails.

## Configuration

Base configuration is in `src/main/resources/application.properties`; provider settings are in `application-local.properties` and `application-cloud.properties`.

Important local variables:

```text
DATABASE_URL
POSTGRES_USER
POSTGRES_PASSWORD
OIDC_ISSUER_URI
OIDC_JWK_SET_URI
OIDC_AUDIENCE
OLLAMA_BASE_URL
OLLAMA_CHAT_MODEL
OLLAMA_EMBEDDING_MODEL
CORS_ALLOWED_ORIGINS
PORT
JOURNAL_WRITES_PER_HOUR
RAG_QUERIES_PER_HOUR
API_REQUESTS_PER_MINUTE
```

Important cloud variables:

```text
SPRING_PROFILES_ACTIVE=cloud
GEMINI_API_KEY_SECRET=projects/.../secrets/.../versions/latest
GOOGLE_CLOUD_PROJECT
FIRESTORE_DATABASE_ID
CORS_ALLOWED_ORIGINS
PORT
```

`GEMINI_API_KEY_SECRET` is a Secret Manager resource name, never the raw key. The key is retrieved once per instance and sent to Google in the `x-goog-api-key` header.

## API

| Method | Route | Request |
|---|---|---|
| POST | `/api/journal/entry` | `{ "content": "..." }` |
| GET | `/api/journal/entries` | none |
| POST | `/api/journal/entries/{id}/retry` | none; failed entries only |
| POST | `/api/chat/rag` | `{ "query": "..." }` |
| GET | `/api/action-items` | none |
| PATCH | `/api/action-items/{id}` | `{ "status": "PENDING" | "COMPLETED" }` |
| DELETE | `/api/action-items/{id}` | none |
| DELETE | `/api/account` | none |
| GET | `/actuator/health` | public |

The older `/api/chat` and `/api/journal-entries` routes remain compatibility endpoints. New frontend code uses the contract above.

## Build and test

```powershell
& .\mvnw.cmd test
& .\mvnw.cmd -DskipTests package
docker build --progress=plain -t personal-gemini-journal-backend .
```

Unit tests cover sanitization, UID propagation, RAG scoping, error mapping, Firebase token failures, local JWT audience/subject validation, Ollama response parsing, outbox success/retry behavior, Firestore ID validation, and vector math. A pgvector integration test is enabled when Testcontainers can reach Docker. Root `scripts/local-smoke.ps1` performs the live authenticated two-user check.

## Error contract

- `400`: invalid JSON, validation, status, or document ID.
- `401`: missing or invalid bearer token.
- `404`: absent or non-owned resource.
- `429`: authenticated-user quota exceeded; includes `Retry-After`.
- `503`: AI/database downstream state represented by `IllegalStateException`.
- `500`: unexpected failure.

Problem Details are deliberately generic; server exceptions and private content are not reflected to the client.
