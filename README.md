# Personal Gemini Journal

Personal Gemini Journal is a private reflection, memory retrieval, and accountability application. A signed-in user can write a journal entry, receive an empathetic AI response, ask questions grounded in their own earlier entries, and track goals extracted from their writing.

The repository is deliberately local-first during the build phase. Keycloak and PostgreSQL/pgvector run locally. The recommended development mode uses a server-side Google AI Studio key with Gemini Flash for generation and Gemini Embedding for private vector retrieval; Ollama remains an optional fully local fallback. The same application exposes a `cloud` Spring profile for the planned Firebase Authentication, Firestore, Gemini, Secret Manager, and Cloud Run deployment.

## Aim

The project demonstrates that useful AI features do not require weakening ownership boundaries. Identity is established by a verified signed token, the backend derives the owner identifier from that token, and persistence adapters scope every data operation to that identifier. The browser never chooses a UID, receives an AI credential, or accesses the server database directly.

## Main capabilities

- Authenticated personal journal with empathetic AI reflection.
- Bounded session chat history plus timestamp-aware private journal context.
- Vector embeddings stored with every entry.
- "Chat with Past Self" using private similarity retrieval and grounded generation.
- Persistence-first journal writes: HTTP 202 after durable save, then background reflection, embedding, and goal extraction.
- One structured Gemini reflection call returns both empathy and 0-3 user-confirmable goal proposals, reducing quota use and preventing a second extraction call from failing independently.
- Gemini generation uses `gemini-3.5-flash-lite`; a `429` rate limit retries once on configurable `gemini-3.1-flash-lite`. Other provider failures are not retried on a second model.
- Durable local processing outbox with bounded retry, stale-job reclamation, and user-triggered retry after terminal AI failure.
- User-confirmed AI goal proposals; model suggestions never become commitments without approval.
- Authenticated per-user quotas for journal writes, RAG, and ordinary API traffic.
- UID-scoped permanent account-data deletion, including Firebase identity deletion in cloud mode.
- Cursor-paginated journal and action-item feeds with stable opaque cursors.
- On-demand, grounded weekly reflections over one authenticated seven-day window.
- Privacy takeout as streamed JSON or Markdown without embeddings or internal jobs.
- Explicitly opt-in location pins that open through keyless Google Maps URLs.
- Colorful month calendar with day-level private memory previews.
- Trusted-claim RBAC (`USER` and `ADMIN`) without weakening per-UID journal ownership.
- Explainable hybrid RAG that reports temporal SQL, location hybrid, semantic hybrid, or lexical fallback retrieval.
- Local and cloud adapters behind the same application ports.
- Responsive Google-inspired blue/red/yellow/green journal UI, secure session handling, CSP, and container health checks.

## Repository layout

```text
personal-gemini-journal/
|-- personal-gemini-journal-backend/   Spring Boot REST API
|-- personal-gemini-journal-frontend/  React/Vite web application
|-- infra/
|   |-- keycloak/                      Local OIDC realm import
|   `-- postgres/                      Non-superuser database bootstrap
|-- scripts/local-smoke.ps1            Authenticated end-to-end smoke test
|-- docs/                              Architecture, security, and migration guides
|-- docker-compose.yml                 Complete local environment
`-- .env.example                       Local environment template
```

## Architecture at a glance

```text
Browser (React)
  | Authorization Code + PKCE
  v
Keycloak (local) / Firebase Auth (cloud)
  | short-lived signed bearer token
  v
Spring Security -> FirebasePrincipal(subject/uid)
  |
  +-> ChatService -> JournalRepository (durable PENDING entry + outbox)
  |                    `-> background worker -> reflection + embedding + goal proposals
  +-> RAG service -> temporal SQL / location hybrid / vector + lexical fusion -> grounded Gemini
  |
  `-> JournalRepository -> PostgreSQL + pgvector / Firestore
                           `-> users are isolated by UID and RLS/path ownership
```

Spring profiles select adapters:

| Concern | `local` profile | `cloud` profile |
|---|---|---|
| Authentication | Keycloak OIDC JWT | Firebase ID token verification |
| Persistence | PostgreSQL 16 + pgvector | Cloud Firestore |
| AI | Gemini `gemini-3.5-flash-lite`, then `gemini-3.1-flash-lite` only on `429`; Ollama fallback | Same Flash-only Gemini policy |
| Embeddings | Gemini `gemini-embedding-2`, normalized to 768 dimensions | Gemini `gemini-embedding-2` |
| Secrets | Ignored `.env.local` | Google Secret Manager + workload identity |
| Background AI | Transactional outbox worker | Spring `@Async` adapter (managed queue planned) |

Detailed flows are in [Architecture](docs/ARCHITECTURE.md) and [Security](docs/SECURITY.md).

## Local prerequisites

- Docker Desktop with Linux containers enabled.
- PowerShell 7 or Windows PowerShell 5.1.
- At least 4 GB of free Docker memory and roughly 2 GB of free disk space for the first model/image pull.
- JDK 17+ and Node.js 22+ only when running services outside Docker.

No Firebase project or Google Cloud service credentials are required locally. A Gemini key is optional: set `AI_GENERATION_PROVIDER=ollama` for a completely local stack, or use the recommended `gemini` setting with a Google AI Studio key for stronger generative output.

## Quick start

From the repository root:

```powershell
Copy-Item .env.example .env.local
```

Edit `.env.local`, assign strong local values to the three infrastructure secrets, and add a Gemini key when using the recommended hybrid mode:

```text
POSTGRES_PASSWORD=
DB_ADMIN_PASSWORD=
KEYCLOAK_ADMIN_PASSWORD=
AI_GENERATION_PROVIDER=gemini
GEMINI_API_KEY=<your Google AI Studio key>
GEMINI_MODEL=gemini-3.5-flash-lite
GEMINI_FALLBACK_MODEL=gemini-3.1-flash-lite
```

Start the stack:

```powershell
docker compose --env-file .env.local up -d --build
docker compose --env-file .env.local ps
```

The first start downloads local models only when `AI_GENERATION_PROVIDER=ollama`; Gemini mode starts without model downloads. Healthy services are available at:

| Service | URL/port |
|---|---|
| Frontend | `http://localhost:13000` |
| Backend health | `http://localhost:18080/actuator/health` |
| Keycloak | `http://localhost:8180` |
| PostgreSQL | `localhost:55432` |
| Ollama | `http://localhost:11434` |

Open `http://localhost:13000`, choose **Sign in securely**, register a local account, and sign in. Keycloak uses Authorization Code flow with mandatory PKCE; implicit flow and password grant remain disabled.

Run the automated authenticated smoke test:

```powershell
& .\scripts\local-smoke.ps1
```

The script creates disposable local users, verifies the immediate pending response, waits for background reflection, performs RAG, accepts and completes an AI goal proposal, checks a second user cannot see or modify the first user's data, and restores Keycloak's secure client settings in a `finally` block.

Stop containers without deleting journal data:

```powershell
docker compose --env-file .env.local down
```

Deleting named volumes permanently removes local accounts, entries, models, and database state:

```powershell
docker compose --env-file .env.local down --volumes
```

## API contract

All application routes require `Authorization: Bearer <access_token>`.

| Method | Endpoint | Request | Response/purpose |
|---|---|---|---|
| POST | `/api/journal/entry` | `{ "content": "...", "location": { ... } }` | `202`; durable entry; location is optional |
| GET | `/api/journal/entries?limit=20&cursor=...` | none | Cursor page of only the caller's entries |
| GET | `/api/journal/calendar?year=2026&month=9&timeZone=Asia/Kolkata` | none | At most 100 owned entries in the selected local-calendar month |
| POST | `/api/journal/entries/{id}/retry` | none | `202`; retries only an owned failed entry |
| POST | `/api/chat/rag` | `{ "query": "...", "timeZone": "Asia/Kolkata", "history": [{ "role": "user" | "model", "text": "..." }] }` | Grounded reply, references, and `retrievalMode` |
| POST | `/api/reflections/weekly` | `{ "timeZone": "Asia/Calcutta" }` | Grounded current-week patterns and focus |
| GET | `/api/action-items?limit=50&cursor=...` | none | Cursor page of owned `PROPOSED`, `PENDING`, and `COMPLETED` items |
| POST | `/api/action-items` | `{ "goal": "..." }` | Creates an owned user-authored `PENDING` goal |
| PATCH | `/api/action-items/{id}` | `{ "status": "PENDING" | "COMPLETED" }` | Accepts or updates an owned goal |
| DELETE | `/api/action-items/{id}` | none | Deletes an owned goal |
| GET | `/api/user/export?format=json|markdown` | none | Streamed private data takeout attachment |
| DELETE | `/api/account` | none | Permanently deletes caller-owned application data and cloud identity |
| GET | `/api/user/me` | none | Verified subject and effective `USER`/`ADMIN` roles |
| GET | `/api/admin/status` | none | Non-sensitive RBAC proof; requires `ADMIN` |
| GET | `/actuator/health` | none | Public liveness/readiness endpoint |

UID is intentionally absent from every request contract.

## How hybrid memory retrieval behaves

| Example question | Strategy | Why |
|---|---|---|
| "What did I do yesterday?" | `TEMPORAL_SQL` | Exact time range is more reliable than semantic similarity. |
| "Where did I have pasta?" | `LOCATION_HYBRID` | Location-label text results are prioritized and fused with vector matches. |
| "What helps me feel productive?" | `SEMANTIC_HYBRID` | Vector meaning and lexical matches are deduplicated into one evidence set. |
| Any semantic question while embeddings return `429` | `LEXICAL_SQL_FALLBACK` | The private journal remains usable and AI reflection persistence is unaffected. |

The UI displays this mode beside every Past Self answer. Every branch is bounded and scoped to the verified UID.

## Security design summary

- Spring Security verifies signature, issuer, expiry, subject format, and required audience before controllers run.
- Firebase cloud verification enables revoked-token checking.
- PostgreSQL uses a non-superuser application role, UID predicates, parameterized SQL, and forced row-level security.
- Firestore paths are always `users/{verifiedUid}/journal_entries` or `users/{verifiedUid}/action_items`.
- AI prompts mark journal content as untrusted data; structured results are bounded and validated.
- Locations are explicit-consent coordinates, server range validated, exported/deleted with entries, and never require a Maps key in the current UI.
- Gemini keys are loaded only from Secret Manager and sent through `x-goog-api-key`, never a URL or browser bundle.
- CORS is an allowlist, tokens are kept in provider memory, and the client refreshes once after `401`.
- Containers bind development ports to loopback, backend and frontend run as non-root users, and Nginx sends CSP/clickjacking/content-type/referrer headers.
- Error responses use sanitized Problem Details and never return downstream exception text.
- Rate limits use the verified UID, return `429` plus `Retry-After`, and expose no client-controlled quota key.
- Destructive account deletion derives ownership from the principal and deletes data before the cloud identity, making partial failure retryable.

See [Security and threat model](docs/SECURITY.md) for trust boundaries, abuse cases, and residual risks.

## Verification performed

The current local build was verified with:

- Maven unit/security/service tests passing.
- PostgreSQL/Flyway migration applied successfully as the non-superuser app role.
- Live health endpoint `UP` and unauthenticated API response `401`.
- Vite 8 production build passing and `npm audit` reporting zero vulnerabilities.
- Backend and frontend Docker images building successfully.
- The previously verified authenticated smoke covered journal, RAG, accountability, update, and cross-user isolation. The revised async/proposal smoke script is ready but requires Docker Desktop to be running for live re-verification.
- Frontend served by UID `101` (unprivileged Nginx) with CSP and related security headers.

## Cloud phase status

Cloud adapters already exist, but deployment is intentionally deferred. Google Cloud work still requires a project with Firebase Authentication, Firestore, Secret Manager, Gemini API access, Artifact Registry/Cloud Build or an equivalent image workflow, a Cloud Run service identity, production CORS/redirect origins, deployed Firestore rules, observability, and the challenge label `dev-tutorial=cloud-run-ai-challenge`.

Follow [Cloud Run migration plan](docs/CLOUD_RUN_MIGRATION.md) when credentials and billing safeguards are ready. That guide separates prerequisites, least-privilege IAM, migration steps, validation, rollback, and cost controls.

## More documentation

- [Local development and troubleshooting](docs/LOCAL_DEVELOPMENT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Security and threat model](docs/SECURITY.md)
- [Cloud Run migration plan](docs/CLOUD_RUN_MIGRATION.md)
- [AI and Google Maps development directives](docs/AI_DEVELOPMENT_DIRECTIVES.md)
- [Backend guide](personal-gemini-journal-backend/README.md)
- [Frontend guide](personal-gemini-journal-frontend/README.md)
