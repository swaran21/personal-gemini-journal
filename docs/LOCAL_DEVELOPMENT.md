# Local development and troubleshooting

## Full Docker workflow

```powershell
Copy-Item .env.example .env.local
# Fill POSTGRES_PASSWORD, DB_ADMIN_PASSWORD, KEYCLOAK_ADMIN_PASSWORD, and GEMINI_API_KEY.
docker compose --env-file .env.local config --quiet
docker compose --env-file .env.local up -d --build
docker compose --env-file .env.local ps
```

Open `http://localhost:13000`. In the recommended Gemini mode, both generation and embeddings use Gemini and Ollama pulls no models. The `ollama` profile remains available for a fully local fallback.

After sign-in, verify the normal user flow in this order: write an entry, wait for its reflection, open Memory Calendar, ask Past Self one temporal and one semantic question, accept a proposed goal, generate the weekly reflection, then export JSON.

### Gemini key for local development

Create a key in Google AI Studio and put it only in the ignored root `.env.local` file:

```text
AI_GENERATION_PROVIDER=gemini
GEMINI_API_KEY=<paste the key here>
GEMINI_MODEL=gemini-3.6-flash
GEMINI_EMBEDDING_MODEL=gemini-embedding-001
GEMINI_EMBEDDING_DIMENSIONS=768
```

Never use a `VITE_*` variable for this key: Vite variables are downloadable by every browser. Never commit `.env.local`. After changing Compose environment values, recreate the backend because Compose reads them when creating the container:

```powershell
docker compose --env-file .env.local up -d --build --force-recreate backend
```

The key is used for journal reflection, RAG answer generation, weekly reflection, action-item extraction, and embeddings. `gemini-embedding-001` is requested at 768 dimensions to match the local pgvector schema and is normalized before cosine search. To run without any external AI request, set `AI_GENERATION_PROVIDER=ollama` and leave `GEMINI_API_KEY` empty; the smaller local model will usually produce less capable summaries.

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
SPRING_PROFILES_ACTIVE=local,gemini
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
GEMINI_API_KEY=<your Google AI Studio key>
GEMINI_MODEL=gemini-3.6-flash
GEMINI_EMBEDDING_MODEL=gemini-embedding-001
GEMINI_EMBEDDING_DIMENSIONS=768
CORS_ALLOWED_ORIGINS=http://localhost:13000,http://localhost:3000
JOURNAL_WRITES_PER_HOUR=30
RAG_QUERIES_PER_HOUR=20
API_REQUESTS_PER_MINUTE=120
```

`PORT=18080` is used because this workstation already had another Java process on `8080`. Spring still defaults to Cloud Run-compatible `${PORT:8080}`.

For the all-local fallback, use `SPRING_PROFILES_ACTIVE=local` and omit `GEMINI_API_KEY`; `OllamaAiService` then handles both generation and embeddings.

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

### Local roles

Every successfully verified account receives application role `USER`. To test the protected admin proof endpoint, open `http://localhost:8180/admin`, choose realm `journal`, select the user, open **Role mapping**, and assign realm role `journal-admin`. Sign out and sign in again so Keycloak issues a new token. The header then shows **Admin** and this request succeeds:

```powershell
# Use a current access token acquired through the normal browser/OIDC flow.
Invoke-RestMethod http://localhost:18080/api/admin/status -Headers @{ Authorization = "Bearer $accessToken" }
```

A normal user receives `403`. Admin role never grants access to another user's entries; repository UID and RLS controls still apply.

### Verify calendar and hybrid RAG

Create entries such as:

```text
Studied Spring Security at the college library.  Location label: College Library
Finished the RAG implementation at home.       Location label: Home
```

Then try:

```text
What did I do today?                  -> TEMPORAL_SQL
Where did I study Spring Security?    -> LOCATION_HYBRID
What technical work gave me confidence? -> SEMANTIC_HYBRID
```

The Past Self response shows the selected retrieval mode and referenced memories. If Gemini embedding quota returns `429`, the mode becomes `LEXICAL_SQL_FALLBACK`; reflection and journal persistence still complete.

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
