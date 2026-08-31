# Personal Gemini Journal

Personal Gemini Journal is a private reflection, memory retrieval, and accountability application. A signed-in user can write a journal entry, receive an empathetic AI response, ask questions grounded in their own earlier entries, and track goals extracted from their writing.

The repository is deliberately local-first during the build phase. It runs without Google Cloud credentials or billing by using Keycloak, PostgreSQL with pgvector, and Ollama. The same application exposes a `cloud` Spring profile for the planned Firebase Authentication, Firestore, Gemini, Secret Manager, and Cloud Run deployment.

## Aim

The project demonstrates that useful AI features do not require weakening ownership boundaries. Identity is established by a verified signed token, the backend derives the owner identifier from that token, and persistence adapters scope every data operation to that identifier. The browser never chooses a UID, receives an AI credential, or accesses the server database directly.

## Main capabilities

- Authenticated personal journal with empathetic AI reflection.
- Multi-turn context from the caller's recent entries.
- Vector embeddings stored with every entry.
- "Chat with Past Self" using private similarity retrieval and grounded generation.
- Durable local accountability outbox with retry and idempotent goal writes.
- Pending/completed accountability dashboard with optimistic UI updates.
- Local and cloud adapters behind the same application ports.
- Responsive warm journal UI, secure session handling, CSP, and container health checks.

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
  +-> ChatService -> GenerativeAiService -> Ollama / Gemini
  |              `-> EmbeddingService ----> Ollama / Gemini Embeddings
  |
  `-> JournalRepository -> PostgreSQL + pgvector / Firestore
                           `-> users are isolated by UID and RLS/path ownership
```

Spring profiles select adapters:

| Concern | `local` profile | `cloud` profile |
|---|---|---|
| Authentication | Keycloak OIDC JWT | Firebase ID token verification |
| Persistence | PostgreSQL 16 + pgvector | Cloud Firestore |
| AI | Ollama `gemma3:1b` | Gemini `gemini-2.5-flash` |
| Embeddings | Ollama `nomic-embed-text` | Gemini Embedding API |
| Secrets | Ignored `.env.local` | Google Secret Manager + workload identity |
| Accountability | Transactional outbox worker | Spring `@Async` post-save extraction |

Detailed flows are in [Architecture](docs/ARCHITECTURE.md) and [Security](docs/SECURITY.md).

## Local prerequisites

- Docker Desktop with Linux containers enabled.
- PowerShell 7 or Windows PowerShell 5.1.
- At least 4 GB of free Docker memory and roughly 2 GB of free disk space for the first model/image pull.
- JDK 17+ and Node.js 22+ only when running services outside Docker.

No Firebase project, Google Cloud credentials, Gemini key, or billing account is required for the local profile.

## Quick start

From the repository root:

```powershell
Copy-Item .env.example .env.local
```

Edit `.env.local` and assign strong local values to these three blank settings:

```text
POSTGRES_PASSWORD=
DB_ADMIN_PASSWORD=
KEYCLOAK_ADMIN_PASSWORD=
```

Start the stack:

```powershell
docker compose --env-file .env.local up -d --build
docker compose --env-file .env.local ps
```

The first start downloads the local chat and embedding models and can take several minutes. Healthy services are available at:

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

The script creates disposable local users, submits a journal entry, performs RAG, waits for outbox-created action items, toggles status, checks a second user cannot see or modify the first user's data, and restores Keycloak's secure client settings in a `finally` block.

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
| POST | `/api/journal/entry` | `{ "content": "..." }` | Saves entry and returns `id`, `aiResponse`, `createdAt` |
| GET | `/api/journal/entries` | none | Lists only the caller's entries |
| POST | `/api/chat/rag` | `{ "query": "..." }` | Returns `reply` and `referencedEntries` |
| GET | `/api/action-items` | none | Lists only the caller's goals |
| PATCH | `/api/action-items/{id}` | `{ "status": "COMPLETED" }` | Updates an owned goal |
| DELETE | `/api/action-items/{id}` | none | Deletes an owned goal |
| GET | `/actuator/health` | none | Public liveness/readiness endpoint |

UID is intentionally absent from every request contract.

## Security design summary

- Spring Security verifies signature, issuer, expiry, subject format, and required audience before controllers run.
- Firebase cloud verification enables revoked-token checking.
- PostgreSQL uses a non-superuser application role, UID predicates, parameterized SQL, and forced row-level security.
- Firestore paths are always `users/{verifiedUid}/journal_entries` or `users/{verifiedUid}/action_items`.
- AI prompts mark journal content as untrusted data; structured results are bounded and validated.
- Gemini keys are loaded only from Secret Manager and sent through `x-goog-api-key`, never a URL or browser bundle.
- CORS is an allowlist, tokens are kept in provider memory, and the client refreshes once after `401`.
- Containers bind development ports to loopback, backend and frontend run as non-root users, and Nginx sends CSP/clickjacking/content-type/referrer headers.
- Error responses use sanitized Problem Details and never return downstream exception text.

See [Security and threat model](docs/SECURITY.md) for trust boundaries, abuse cases, and residual risks.

## Verification performed

The current local build was verified with:

- Maven unit/security/service tests passing.
- PostgreSQL/Flyway migration applied successfully as the non-superuser app role.
- Live health endpoint `UP` and unauthenticated API response `401`.
- Vite 8 production build passing and `npm audit` reporting zero vulnerabilities.
- Backend and frontend Docker images building successfully.
- Full authenticated smoke result: journal, RAG, accountability, update, and cross-user isolation all pass.
- Frontend served by UID `101` (unprivileged Nginx) with CSP and related security headers.

## Cloud phase status

Cloud adapters already exist, but deployment is intentionally deferred. Google Cloud work still requires a project with Firebase Authentication, Firestore, Secret Manager, Gemini API access, Artifact Registry/Cloud Build or an equivalent image workflow, a Cloud Run service identity, production CORS/redirect origins, deployed Firestore rules, observability, and the challenge label `dev-tutorial=cloud-run-ai-challenge`.

Follow [Cloud Run migration plan](docs/CLOUD_RUN_MIGRATION.md) when credentials and billing safeguards are ready. That guide separates prerequisites, least-privilege IAM, migration steps, validation, rollback, and cost controls.

## More documentation

- [Local development and troubleshooting](docs/LOCAL_DEVELOPMENT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Security and threat model](docs/SECURITY.md)
- [Cloud Run migration plan](docs/CLOUD_RUN_MIGRATION.md)
- [Backend guide](personal-gemini-journal-backend/README.md)
- [Frontend guide](personal-gemini-journal-frontend/README.md)
