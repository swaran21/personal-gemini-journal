# Project contract

## Layout and profiles

- `personal-gemini-journal-backend/`: Spring Boot 3 API.
- `personal-gemini-journal-frontend/`: React/Vite UI.
- `local`: Keycloak, PostgreSQL/pgvector, Ollama, transactional outbox.
- `cloud`: Firebase token verification, Firestore, Gemini, Secret Manager; Cloud Run deployment remains deferred.

## Ownership and API rules

- Every `/api/**` route requires a verified bearer token.
- Controllers obtain `FirebasePrincipal.uid()` and pass it downward.
- Request schemas contain no UID.
- Cursor pages use opaque `createdAt + id` cursors and limits `1..100`.
- Firestore collections remain under `users/{uid}/journal_entries` and `users/{uid}/action_items`.
- Journal creation returns `202 PENDING`; AI processing is background work.
- AI goal output remains `PROPOSED` until user acceptance.

## Current feature endpoints

- `POST /api/journal/entry`
- `GET /api/journal/entries?limit=&cursor=`
- `POST /api/journal/entries/{id}/retry`
- `POST /api/chat/rag`
- `POST /api/reflections/weekly`
- `GET /api/action-items?limit=&cursor=`
- `PATCH|DELETE /api/action-items/{id}`
- `GET /api/user/export?format=json|markdown`
- `DELETE /api/account`

## Verification

From `personal-gemini-journal-backend`:

```powershell
& .\mvnw.cmd -q test
& .\mvnw.cmd -q -DskipTests package
```

From `personal-gemini-journal-frontend`:

```powershell
npm run build
```

From the repository root:

```powershell
docker compose --env-file .env.local config --quiet
& .\scripts\local-smoke.ps1
```

Testcontainers tests may skip when Docker is unavailable; report that separately from passing unit tests.
