# Cloud Run migration plan

The local application is complete and verified. This document lists cloud work that is intentionally not performed until a valid Google Cloud project, billing safeguards, and credentials are available.

## What is already implemented

- `cloud` Spring profile.
- Firebase Admin ID-token verification with revocation checking.
- Firestore adapter with strict `users/{uid}/journal_entries` and `users/{uid}/action_items` paths.
- Gemini reflection, extraction, grounding, and embedding adapters.
- Secret Manager lookup using a full secret-version resource name.
- Cloud Run-compatible dynamic `PORT` and forwarded-header handling.
- Firebase frontend authentication provider with per-request token refresh.
- Multi-stage non-root backend container.

These adapters compile and have unit tests, but they are not considered live-verified until run in the target Google Cloud project.

## Prerequisites and cost guardrails

The challenge instructions require a Google Cloud project with billing enabled. Billing enabled does not mean money must be spent, but it permits chargeable usage. Before provisioning:

1. Create a dedicated project, not a shared production project.
2. Set a small budget and email alerts. Budgets alert; they do not automatically stop spend.
3. Set Cloud Run maximum instances and conservative CPU/memory/concurrency.
4. Set Gemini/API quotas where available.
5. Review Firestore, Secret Manager, Artifact Registry, Cloud Build, egress, and Gemini pricing/free allowances.
6. Delete challenge resources after judging if they are no longer needed.

## Migration mapping

| Local component | Google Cloud target |
|---|---|
| Keycloak | Firebase Authentication with Google provider |
| PostgreSQL/pgvector | Cloud Firestore initially; optionally Firestore vector search/Vertex AI Vector Search later |
| Ollama chat | Gemini API `gemini-2.5-flash` |
| Ollama embeddings | Gemini Embedding API |
| `.env.local` | Secret Manager plus Cloud Run environment variables |
| Docker Compose | Cloud Run services and managed dependencies |
| Local outbox | Pub/Sub or Cloud Tasks for production durability |

## Provisioning sequence

1. Create/select the project and attach the guarded billing account.
2. Enable Firebase Authentication and Google Sign-In.
3. Create Firestore in the chosen region/database ID.
4. Enable Secret Manager, Cloud Run, Artifact Registry, build API, and Gemini/Generative Language access.
5. Store the raw Gemini key as a Secret Manager secret; never store it in Git or a Vite variable.
6. Create a dedicated Cloud Run service account.
7. Grant only secret accessor for the one Gemini secret and the minimum Firestore role required by this app.
8. Build and push the backend image; scan it before deployment.
9. Deploy with `SPRING_PROFILES_ACTIVE=cloud`, `GOOGLE_CLOUD_PROJECT`, `FIRESTORE_DATABASE_ID`, `GEMINI_API_KEY_SECRET`, and exact production `CORS_ALLOWED_ORIGINS`.
10. Build the frontend with `VITE_AUTH_MODE=firebase`, Firebase public web config, and the HTTPS backend URL.
11. Deploy frontend separately or serve it through an appropriate static/Cloud Run service.
12. Add production domains to Firebase authorized domains and backend CORS.

## Firestore rules and indexes

Deploy deny-by-default rules that allow access only when `request.auth.uid == userId`, validate document keys/types/lengths, and allow only intended update fields. Admin SDK bypasses rules, so backend path scoping remains mandatory. Test rules with the Firestore emulator and two distinct users.

Create any required ordering/vector indexes before load testing. Confirm a user's RAG query cannot issue a global collection-group search.

## Production reliability changes

Before public use, replace cloud `@Async` accountability extraction with a durable Pub/Sub/Cloud Tasks event containing a UID, entry ID, and idempotency key. The consumer must re-read the entry only from the UID path, write deduplicated goals under that UID, retry transient failures, and dead-letter permanent failures.

Add timeouts, retry policies with jitter, circuit breaking, per-user quotas, request correlation IDs, structured metrics, and alerts for authentication failures, AI latency/errors, outbox backlog, and Firestore errors.

## Validation checklist

- Firebase token from the wrong project -> `401`.
- Revoked/expired/malformed token -> `401`.
- Missing token -> `401`.
- User A cannot list, read, update, or delete User B data.
- Blank/oversized/malformed input -> `400`.
- Gemini/Firestore outage -> sanitized `503`/`500`, no secret or journal text leak.
- CORS permits only the production frontend and required headers/methods.
- Cloud Run liveness/readiness and dynamic `PORT` work.
- Secret is absent from image history, environment dumps, browser bundle, URLs, and logs.
- Cold-start, concurrency, timeout, and maximum-instance settings are measured.
- Export/delete/retention behavior matches the privacy notice.

## Challenge deployment tasks

After successful production smoke testing:

1. Publish the application to Cloud Run.
2. Find the deployed service in the Cloud Run console.
3. Add label key `dev-tutorial` with value `cloud-run-ai-challenge`.
4. Verify the public URL, sign-in, journal persistence, logout/login persistence, RAG, and accountability workflow.
5. Record architecture, threat model, test evidence, and unique enhancements for the submission.

## Rollback

Keep the previous Cloud Run revision available, use immutable image tags/digests, and route traffic back if authentication, isolation, or persistence checks fail. Do not migrate/delete local data until cloud export/import and user consent requirements are defined.
