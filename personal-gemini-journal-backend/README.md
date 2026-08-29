# Personal Gemini Journal Backend

Secure Spring Boot 3 REST API for the Personal Gemini Journal. The service validates Firebase ID tokens, isolates Firestore data by Firebase UID, calls Gemini for reflection and retrieval-augmented generation, and extracts accountability items asynchronously.

## Responsibilities

- Authenticate every application request with Firebase Admin.
- Validate and sanitize journal and RAG input.
- Persist journal text, AI response, timestamps, and embedding vectors.
- Answer questions using only the authenticated user's private memories.
- Extract commitments after save and persist owned action items.
- Expose stable JSON contracts for the React frontend.

## Architecture

```text
Firebase ID token
        ↓
FirebaseAuthenticationFilter → FirebasePrincipal(uid)
        ↓
Controllers → ChatService / AccountabilityService
        ↓                         ↓
JournalRepository          GeminiService / EmbeddingService
        ↓                         ↓
Firestore user collections      Gemini REST API
```

Package responsibilities:

| Package | Responsibility |
|---|---|
| `security` | Firebase verification, principal creation, stateless security policy |
| `config` | Firebase/Firestore clients, Secret Manager, RestClient, async execution |
| `chat` | Chat and RAG endpoints, DTOs, orchestration |
| `gemini` | Reflection, structured extraction, and embedding adapters |
| `journal` | HTTP DTOs, Firestore models, and repository persistence |
| `common` | Problem Details and sanitized exception responses |

## Authentication and threat model

Clients send `Authorization: Bearer <Firebase ID token>`. The filter verifies signature, issuer, audience, expiry, and revocation with Firebase Admin. Only the resulting `FirebasePrincipal.uid()` is used by controllers and repositories. A UID supplied in JSON, a URL, or a query string is ignored because no such ownership input exists.

The API is stateless and does not use form login, server sessions, or browser cookies. The health endpoint is public; journal, chat, and action-item endpoints are protected. Unexpected errors are logged server-side while responses omit internal exception details.

## Firestore isolation

Every operation is below one of these paths:

```text
users/{uid}/journal_entries/{entryId}
users/{uid}/action_items/{actionItemId}
```

`JournalRepository` constructs paths from the verified UID. Entry listing, embedding retrieval, action-item listing, updates, and deletes are all scoped to that path. There are no global collection queries and no client-controlled collection paths. Keep Firestore security rules deployed as defense in depth.

## AI pipeline

### Journal entry

`POST /api/journal/entry` accepts validated `content` up to 10,000 characters. The service sanitizes control characters, loads recent entries from the same UID for conversational continuity, asks `gemini-2.5-flash` for an empathetic response, generates a `gemini-embedding-001` vector, and saves the result.

### Chat with Past Self

`POST /api/chat/rag` accepts `query` (legacy `question` is also accepted). The query is embedded, private entries are loaded from the caller's collection, cosine similarity ranks candidates, and the best bounded matches are sent as grounding context to Gemini. The result includes the answer and bounded excerpts from those referenced entries.

### Accountability agent

After saving an entry, `AccountabilityService.extractAndPersist` runs with Spring `@Async`. Gemini extracts concrete goals, commitments, and deadlines. Each valid item is stored with `PENDING` status under the same UID. This workflow is intentionally non-blocking; clients should reload action items after extraction completes.

## API reference

All routes below require the Firebase bearer token unless explicitly stated.

| Method | Route | Request | Response |
|---|---|---|---|
| POST | `/api/journal/entry` | `{ "content": "..." }` | `id`, `content`, `aiResponse`, `extractedGoal`, `createdAt` |
| GET | `/api/journal/entries` | — | Array of the caller's entries |
| POST | `/api/chat/rag` | `{ "query": "..." }` | `reply`, `referencedEntries` |
| GET | `/api/action-items` | — | Array of owned action items |
| PATCH | `/api/action-items/{id}` | `{ "status": "COMPLETED" }` | `204 No Content` |
| DELETE | `/api/action-items/{id}` | — | `204 No Content` |
| GET | `/actuator/health` | — | Health status; public |

Timestamps are ISO-8601 strings. Valid action statuses are exactly `PENDING` and `COMPLETED`. `extractedGoal` may be null because goal extraction is asynchronous.

## Dependencies

The Maven build uses Spring Web, Security, Validation, and Actuator; Firebase Admin SDK; Google Cloud Firestore and Secret Manager; and Spring test/security-test modules. Gemini is called through Spring `RestClient`, so no Gemini key is embedded in the artifact.

## Configuration and secrets

`application.properties` reads `PORT`, `GEMINI_API_KEY_SECRET`, `GOOGLE_CLOUD_PROJECT`, `FIRESTORE_DATABASE_ID`, `GEMINI_MODEL`, `GEMINI_EMBEDDING_MODEL`, and `CORS_ALLOWED_ORIGINS` from the environment. The Gemini value is a Secret Manager resource name such as `projects/PROJECT_ID/secrets/GEMINI_API_KEY/versions/latest`, not the raw key.

For local development, copy `src/main/resources/application-local.properties.example` to `application-local.properties`. This file is ignored by Git. Authenticate with Google Application Default Credentials using `gcloud auth application-default login`; the local identity needs Secret Manager and Firestore access.

## Run and test

From this directory, run `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local` to start on port 8080, or `.\mvnw.cmd test` to run the automated suite. Package with `.\mvnw.cmd clean package`. The application honors Cloud Run's dynamic `PORT` variable through `server.port=${PORT:8080}`.

## Container and Cloud Run

The Dockerfile uses Eclipse Temurin 17 JRE Alpine and copies only the packaged JAR. Build with `docker build --progress=plain -t personal-gemini-journal-api .`. Deploy using a dedicated service account with Secret Manager Secret Accessor and Firestore permissions. Set `GEMINI_API_KEY_SECRET`, `GOOGLE_CLOUD_PROJECT`, `FIRESTORE_DATABASE_ID`, and production `CORS_ALLOWED_ORIGINS` as runtime configuration.

## Error behavior

Malformed JSON, blank/oversized text, and invalid statuses return `400 Bad Request`. Missing, malformed, expired, revoked, or invalid-project tokens return `401 Unauthorized`. Missing resources under the caller's collection return `404`. Unexpected failures return sanitized `500 Internal Server Error` responses.

## Testing and production checklist

- Run `.\mvnw.cmd clean test` before every release.
- Use Firebase/Firestore emulators or a dedicated test project for integration tests.
- Confirm Firestore rules deny all unauthorized and cross-user access.
- Confirm the Cloud Run identity can access only the required secret and database.
- Set a production frontend origin instead of using the localhost CORS default.
- Monitor `/actuator/health`, Gemini failures, and asynchronous extraction failures.
