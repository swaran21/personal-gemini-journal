# Personal Gemini Journal API

Spring Boot 3 API for Firebase-authenticated journaling on Cloud Run. `POST /api/chat` requires `Authorization: Bearer <Firebase ID token>` and body `{"entry":"..."}`. It returns `{"reply":"...","extractedGoal":"..."}`; the first goal is returned immediately and all extracted goals are persisted asynchronously.

The frontend should use authenticated API calls rather than the Firestore Web SDK: `GET /api/journal-entries`, `GET /api/action-items`, `PATCH /api/action-items/{id}` with `{"completed":true}`, and `DELETE /api/action-items/{id}`. Each endpoint uses the JWT UID, never a client-supplied UID.

## Security model

The Firebase Admin SDK verifies every bearer token (including token revocation). The verified UID is the only identifier passed into repository methods. Repository path construction is fixed to `users/{uid}/journal_entries` and `users/{uid}/action_items`; request data never supplies a collection path or user ID. Configure the supplied Firestore rules too if the browser continues to read/write Firestore directly; preferably migrate those reads and item mutations behind this API.

Gemini keys are read at runtime from Secret Manager. Set `GEMINI_API_KEY_SECRET=projects/PROJECT_ID/secrets/GEMINI_API_KEY/versions/latest`; do not set an API-key environment variable. Grant the Cloud Run service account `roles/secretmanager.secretAccessor`, Firestore access, and Firebase Admin-compatible Google credentials.

## Run and deploy

Use application default credentials locally (`gcloud auth application-default login`), set `GEMINI_API_KEY_SECRET`, then run `./mvnw spring-boot:run`. Build with `./mvnw clean package`, then `docker build --progress=plain -t journal-api .`.

For Cloud Run, deploy with a dedicated service account and set only non-secret configuration such as `CORS_ALLOWED_ORIGINS=https://your-frontend.example` and `GEMINI_API_KEY_SECRET=projects/.../versions/latest`. Cloud Run provides `PORT`; Spring Boot will honor it when `SERVER_PORT=$PORT` is supplied at deployment. Health check: `/actuator/health`.
