# Local development and troubleshooting

## Full Docker workflow

```powershell
Copy-Item .env.example .env.local
# Fill POSTGRES_PASSWORD, DB_ADMIN_PASSWORD, and KEYCLOAK_ADMIN_PASSWORD.
docker compose --env-file .env.local config --quiet
docker compose --env-file .env.local up -d --build
docker compose --env-file .env.local ps
```

Open `http://localhost:13000`. First-time model downloads are stored in the `journal-ollama` volume and are reused.

Useful checks:

```powershell
Invoke-RestMethod http://localhost:18080/actuator/health
Invoke-RestMethod http://localhost:8180/realms/journal/.well-known/openid-configuration
Invoke-RestMethod http://localhost:11434/api/tags
docker compose --env-file .env.local logs --tail=200 backend
& .\scripts\local-smoke.ps1
```

## Run backend from IntelliJ

Keep PostgreSQL, Keycloak, and Ollama running in Docker:

```powershell
docker compose --env-file .env.local up -d postgres keycloak ollama
docker compose --env-file .env.local up ollama-models
```

Use the main class:

```text
com.pm.personalgeminijournalbackend.PersonalGeminiJournalBackendApplication
```

Set working directory to `personal-gemini-journal-backend` and environment variables:

```text
SPRING_PROFILES_ACTIVE=local
PORT=18080
DATABASE_URL=jdbc:postgresql://localhost:55432/journal
POSTGRES_USER=journal_app
POSTGRES_PASSWORD=<the value from root .env.local>
OIDC_ISSUER_URI=http://localhost:8180/realms/journal
OIDC_JWK_SET_URI=http://localhost:8180/realms/journal/protocol/openid-connect/certs
OIDC_AUDIENCE=journal-web
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=gemma3:1b
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
CORS_ALLOWED_ORIGINS=http://localhost:13000,http://localhost:3000
JOURNAL_WRITES_PER_HOUR=30
RAG_QUERIES_PER_HOUR=20
API_REQUESTS_PER_MINUTE=120
```

`PORT=18080` is used because this workstation already had another Java process on `8080`. Spring still defaults to Cloud Run-compatible `${PORT:8080}`.

## Run frontend with Vite

```powershell
Set-Location personal-gemini-journal-frontend
Copy-Item .env.example .env.local
npm ci
npm run dev
```

The Vite configuration uses port `3000`. Its `.env.example` selects OIDC and backend port `18080`. Firebase values are required only after changing `VITE_AUTH_MODE=firebase`.

## Tests

```powershell
Set-Location personal-gemini-journal-backend
& .\mvnw.cmd test
& .\mvnw.cmd -DskipTests package

Set-Location ..\personal-gemini-journal-frontend
npm ci
npm audit
npm run build
```

The pgvector integration test uses Testcontainers and skips when Testcontainers cannot reach Docker. The real Compose smoke script remains the authoritative local end-to-end check on Windows.

## Common problems

### Port already in use

Edit `BACKEND_HOST_PORT`, `FRONTEND_HOST_PORT`, or `POSTGRES_HOST_PORT` in `.env.local`. If the frontend port changes, also update the Keycloak redirect origins, backend CORS value, and frontend build configuration.

### Keycloak redirect error

Confirm the browser origin appears in `infra/keycloak/journal-realm.json`. Realm import uses `IGNORE_EXISTING`; changing the JSON does not modify an already-created Keycloak volume. Recreate only the Keycloak volume or update the local client through the admin console.

### No local models

```powershell
docker compose --env-file .env.local up ollama-models
Invoke-RestMethod http://localhost:11434/api/tags
```

### Backend migration failure

Inspect PostgreSQL logs and confirm the app role is not a superuser:

```powershell
docker compose --env-file .env.local logs postgres
docker exec personal-gemini-journal-postgres-1 psql -U journal_app -d journal -tAc "select rolsuper from pg_roles where rolname='journal_app'"
```

The expected result is `f`/`false`.

### Reflection and goals appear after the entry

This is intentional. Journal creation returns after the text and processing job are durable. The UI first shows `PENDING`, then polls for the reflection. Goal extraction produces a `PROPOSED` item that must be accepted before it counts toward progress. Inspect `accountability_outbox` and backend logs if processing does not complete after bounded retries.

If processing reaches `FAILED`, use **Try reflection again** in the journal feed. The backend resets only the owned failed job and reuses the already stored text.

### HTTP 429 responses

Local per-user quotas default to 30 journal writes/hour, 20 RAG queries/hour, and 120 other API calls/minute. A rejected request includes `Retry-After`. Adjust the three rate-limit variables in `.env.local`, then recreate the backend container so Compose loads the new environment.

### Delete local journal data

Use the shield icon, type `DELETE`, and confirm. PostgreSQL removes every row owned by the signed-in subject, including queued jobs. The Keycloak identity remains because Keycloak is an external local identity provider; delete that login separately through `http://localhost:8180/admin` when required.
