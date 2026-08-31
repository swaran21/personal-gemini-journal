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
| `security` | Local OIDC/cloud Firebase authentication, principal mapping, CORS |
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

## RAG and accountability

An entry is reflected on and embedded before save. Local vectors use `vector(768)` and a cosine HNSW index. RAG performs `ORDER BY embedding <=> :queryVector` only after UID filtering. Cloud mode performs bounded in-memory cosine ranking over documents already loaded from the UID path.

Local `saveEntry` also inserts an outbox row in the same transaction. The worker claims jobs with `FOR UPDATE SKIP LOCKED`, reclaims stale jobs, applies exponential retries, and deduplicates action items by owner/source entry/goal. Cloud mode currently uses the hackathon-required `@Async` post-save workflow.

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
| POST | `/api/chat/rag` | `{ "query": "..." }` |
| GET | `/api/action-items` | none |
| PATCH | `/api/action-items/{id}` | `{ "status": "PENDING" | "COMPLETED" }` |
| DELETE | `/api/action-items/{id}` | none |
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
- `503`: AI/database downstream state represented by `IllegalStateException`.
- `500`: unexpected failure.

Problem Details are deliberately generic; server exceptions and private content are not reflected to the client.
