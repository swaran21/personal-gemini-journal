# Personal Gemini Journal

Personal Gemini Journal is a private reflection and accountability application. Signed-in users write entries, receive empathetic Gemini reflections, search their own history with retrieval-augmented generation (RAG), and track commitments extracted from their writing.

## Repository layout

```text
personal-gemini-journal/
├── personal-gemini-journal-backend/   Spring Boot 3 REST API
└── personal-gemini-journal-frontend/  React 18 + Vite web client
```

## Aim

The application makes personal data useful without mixing users' data. Firebase Authentication establishes identity, the backend derives the UID from a verified token, and every Firestore operation is created below that UID. Gemini provides reflection and goal extraction; embeddings enable questions about the user's own past.

## Capabilities

- Google sign-in through Firebase Authentication.
- Validated journal entry creation with an empathetic Gemini response.
- Gemini embeddings saved beside each journal entry.
- “Chat with Past Self” grounded in cosine-ranked private memories.
- Asynchronous accountability extraction into user-owned action items.
- Pending/completed action tracking with optimistic UI updates.
- Responsive warm interface based on the original design.

## Architecture

```text
React/Vite browser
  │ Firebase sign-in → Firebase ID token
  │ Authorization: Bearer <token>
  ▼
FirebaseAuthenticationFilter
  │ verifies token and creates FirebasePrincipal(uid)
  ▼
Controllers → application services → Firestore/Gemini adapters
                              ├─ users/{uid}/journal_entries
                              └─ users/{uid}/action_items
```

The browser never sends a UID and never sends a Gemini key. Gemini credentials are resolved from Google Cloud Secret Manager at runtime.

## Technology

Backend: Java 17, Spring Boot 3.4.8, Spring Web/Security/Validation/Actuator, Firebase Admin SDK, Google Cloud Firestore, Google Cloud Secret Manager, Gemini REST calls through `RestClient`, and Maven Wrapper.

Frontend: React 18, Vite 5, Tailwind CSS 3, Firebase Web SDK 12, and Lucide React.

## API contract

Every application request requires `Authorization: Bearer <Firebase ID token>`.

| Method | Endpoint | Body | Purpose |
|---|---|---|---|
| POST | `/api/journal/entry` | `{ "content": "..." }` | Save, reflect, and embed an entry |
| GET | `/api/journal/entries` | — | List the caller's entries |
| POST | `/api/chat/rag` | `{ "query": "..." }` | Query private memories |
| GET | `/api/action-items` | — | List the caller's goals |
| PATCH | `/api/action-items/{id}` | `{ "status": "PENDING" }` | Change goal status |
| DELETE | `/api/action-items/{id}` | — | Delete an owned goal |
| GET | `/actuator/health` | — | Health/readiness check |

Responses use ISO-8601 timestamps. RAG returns `reply` and referenced entry IDs; action items use `PENDING` or `COMPLETED`.

## Data isolation

```text
users/{firebaseUid}/journal_entries/{entryId}
users/{firebaseUid}/action_items/{actionItemId}
```

Repositories construct these paths internally from the verified principal. There are no global collection queries or client-controlled paths. Keep Firestore security rules deployed as defense in depth.

## Local development

Prerequisites: JDK 17+, Node.js 18+, npm, a Firebase project with Google sign-in enabled, a Google Cloud project with Firestore/Secret Manager/Gemini enabled, and the Google Cloud CLI.

Backend: authenticate with `gcloud auth application-default login`, create `application-local.properties` from the backend example, then run `cd personal-gemini-journal-backend` and `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`. Test with `.\mvnw.cmd test`.

Frontend: copy `personal-gemini-journal-frontend/.env.example` to `.env.local`, fill the Firebase Web App values, then run `cd personal-gemini-journal-frontend`, `npm ci`, and `npm run dev`. The frontend defaults to `http://localhost:3000`; the API defaults to `http://localhost:8080`.

## Secrets and Cloud Run

Never commit `.env.local`, service-account JSON, Gemini API keys, or Firebase Admin private keys. Browser Firebase configuration is client configuration; server credentials belong in Google Cloud IAM and Secret Manager. Build with `cd personal-gemini-journal-backend`, `.\mvnw.cmd clean package`, and `docker build --progress=plain -t personal-gemini-journal-api .`. Cloud Run supplies `PORT`; Spring uses `server.port=${PORT:8080}`. Configure `GEMINI_API_KEY_SECRET`, `GOOGLE_CLOUD_PROJECT`, `FIRESTORE_DATABASE_ID`, and `CORS_ALLOWED_ORIGINS` at deployment.

## Security and operations

Malformed, expired, revoked, or cross-project tokens are rejected. Invalid input returns `400`, authentication failures return `401`, missing owned resources return `404`, and unexpected failures return sanitized `500` responses. Input sizes are bounded and sanitized before Gemini/Firestore use. RAG retrieval is user-scoped, and accountability extraction is asynchronous after the entry save.

See [backend documentation](personal-gemini-journal-backend/README.md) and [frontend documentation](personal-gemini-journal-frontend/README.md) for implementation details.
