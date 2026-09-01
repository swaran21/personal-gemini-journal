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
| Ollama chat | Gemini API `gemini-3.5-flash-lite`, with `gemini-3.1-flash-lite` rate-limit fallback |
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
13. Assign Firebase custom claim `roles: ["journal-admin"]` only through a trusted Admin SDK/IAM-controlled process when an administrator is genuinely required; force token refresh after claim changes.

## Copyable Cloud Run deployment flow

The following PowerShell flow intentionally uses example names. Replace every value in angle brackets. Run it from the repository root after `gcloud auth login` and `gcloud auth application-default login` both succeed. Do not paste a Gemini key directly into a command because shell history may retain it.

```powershell
$projectId = "<your-google-cloud-project-id>"
$region = "asia-south1"
$repository = "journal-images"
$runtimeAccountName = "journal-runtime"
$runtimeAccount = "$runtimeAccountName@$projectId.iam.gserviceaccount.com"
$backendService = "personal-gemini-journal-backend"
$frontendService = "personal-gemini-journal-frontend"
$backendImage = "$region-docker.pkg.dev/$projectId/$repository/backend:v1"
$frontendImage = "$region-docker.pkg.dev/$projectId/$repository/frontend:v1"

gcloud config set project $projectId
gcloud services enable run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com secretmanager.googleapis.com firestore.googleapis.com firebase.googleapis.com generativelanguage.googleapis.com
gcloud iam service-accounts create $runtimeAccountName --display-name="Personal Gemini Journal runtime"
gcloud artifacts repositories create $repository --repository-format=docker --location=$region
gcloud auth configure-docker "$region-docker.pkg.dev"
```

Create `gemini-api-key` in Secret Manager using the Cloud Console, or place the key in a temporary file outside this repository and run the following. Securely delete that file afterward.

```powershell
gcloud secrets create gemini-api-key --replication-policy=automatic
gcloud secrets versions add gemini-api-key --data-file="C:\secure\gemini-api-key.txt"
gcloud secrets add-iam-policy-binding gemini-api-key --member="serviceAccount:$runtimeAccount" --role="roles/secretmanager.secretAccessor"
gcloud projects add-iam-policy-binding $projectId --member="serviceAccount:$runtimeAccount" --role="roles/datastore.user"
```

Build and push the backend. Deploying it with `--allow-unauthenticated` allows browsers to reach Cloud Run; it does **not** remove application authentication. Spring Security still verifies a Firebase ID token on every `/api/**` request.

```powershell
docker build --tag $backendImage .\personal-gemini-journal-backend
docker push $backendImage

gcloud run deploy $backendService `
  --image=$backendImage `
  --region=$region `
  --service-account=$runtimeAccount `
  --allow-unauthenticated `
  --port=8080 `
  --cpu=1 `
  --memory=1Gi `
  --concurrency=20 `
  --max-instances=2 `
  --timeout=300 `
  --labels=dev-tutorial=cloud-run-ai-challenge `
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,GOOGLE_CLOUD_PROJECT=$projectId,FIRESTORE_DATABASE_ID=(default),GEMINI_API_KEY_SECRET=projects/$projectId/secrets/gemini-api-key/versions/latest,GEMINI_MODEL=gemini-3.5-flash-lite,GEMINI_FALLBACK_MODEL=gemini-3.1-flash-lite,GEMINI_EMBEDDING_MODEL=gemini-embedding-2,GEMINI_EMBEDDING_DIMENSIONS=768,CORS_ALLOWED_ORIGINS=https://temporary.invalid"

$backendUrl = gcloud run services describe $backendService --region=$region --format="value(status.url)"
```

Register a Firebase Web App and copy its public web configuration. These values identify the browser app and are not server secrets. Never place the Gemini key, a service-account JSON document, or a Secret Manager value in a `VITE_*` build argument.

```powershell
$firebaseApiKey = "<firebase-web-api-key>"
$firebaseAuthDomain = "$projectId.firebaseapp.com"
$firebaseAppId = "<firebase-web-app-id>"
$firebaseStorageBucket = "<firebase-storage-bucket>"
$firebaseMessagingSenderId = "<firebase-messaging-sender-id>"

docker build --tag $frontendImage `
  --build-arg VITE_API_BASE_URL=$backendUrl `
  --build-arg VITE_AUTH_MODE=firebase `
  --build-arg VITE_FIREBASE_API_KEY=$firebaseApiKey `
  --build-arg VITE_FIREBASE_AUTH_DOMAIN=$firebaseAuthDomain `
  --build-arg VITE_FIREBASE_PROJECT_ID=$projectId `
  --build-arg VITE_FIREBASE_STORAGE_BUCKET=$firebaseStorageBucket `
  --build-arg VITE_FIREBASE_MESSAGING_SENDER_ID=$firebaseMessagingSenderId `
  --build-arg VITE_FIREBASE_APP_ID=$firebaseAppId `
  .\personal-gemini-journal-frontend
docker push $frontendImage

gcloud run deploy $frontendService `
  --image=$frontendImage `
  --region=$region `
  --allow-unauthenticated `
  --port=8080 `
  --cpu=1 `
  --memory=256Mi `
  --concurrency=80 `
  --max-instances=2 `
  --labels=dev-tutorial=cloud-run-ai-challenge

$frontendUrl = gcloud run services describe $frontendService --region=$region --format="value(status.url)"
```

Add `$frontendUrl` to Firebase Authentication **Authorized domains**. Then update the backend with the exact frontend origin; do not use `*` with bearer-token requests. The frontend image builds a CSP containing only its configured backend and identity-provider origins plus Firebase API endpoints.

```powershell
gcloud run services update $backendService `
  --region=$region `
  --update-env-vars="CORS_ALLOWED_ORIGINS=$frontendUrl"
```

### Cloud smoke verification

1. Open `$frontendUrl`, sign in through Google, and confirm the header shows `Secure · User`.
2. Save `I studied Spring Security at the campus library today.` with an approved location.
3. Confirm the entry appears immediately as `PENDING`, then changes to an AI reflection without losing the journal if Gemini is unavailable.
4. Ask `What did I do today?` and expect retrieval mode `TEMPORAL SQL` with only today's entries.
5. Ask `Where did I study Spring Security?` and expect a location-aware hybrid answer with referenced memories.
6. Accept a proposed action, change its status, reload, and confirm it persists.
7. Export JSON and confirm it contains only the signed-in user's entries/action items and omits embeddings.
8. Repeat isolation checks with a second Firebase account before public launch.

## Firestore rules and indexes

Deploy deny-by-default rules that allow access only when `request.auth.uid == userId`, validate document keys/types/lengths, and allow only intended update fields. Admin SDK bypasses rules, so backend path scoping remains mandatory. Test rules with the Firestore emulator and two distinct users.

Create any required ordering/vector indexes before load testing. Confirm a user's RAG query cannot issue a global collection-group search.

## Production reliability changes

Before public use, replace cloud `@Async` journal processing with a durable Pub/Sub/Cloud Tasks event containing a UID, entry ID, and idempotency key. The consumer must re-read the entry only from the UID path, complete reflection/embedding, write deduplicated `PROPOSED` goals under that UID, retry transient failures, and dead-letter permanent failures.

The included rate limiter is deliberately instance-local. Put Cloud Armor/API Gateway or a shared Redis-backed limiter in front of multi-instance Cloud Run, retaining verified-UID quotas for cost control. Add timeouts, retry policies with jitter, circuit breaking, request correlation IDs, structured metrics, and alerts for authentication failures, AI latency/errors, queue backlog, account-deletion failures, and Firestore errors.

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

## Authoritative references

- [Deploying container images to Cloud Run](https://docs.cloud.google.com/run/docs/deploying)
- [`gcloud run deploy` command reference](https://docs.cloud.google.com/sdk/gcloud/reference/run/deploy)
- [Secret Manager access control](https://docs.cloud.google.com/secret-manager/docs/access-control)
- [Manage access to Secret Manager secrets](https://docs.cloud.google.com/secret-manager/docs/manage-access-to-secrets)
- [Verify Firebase ID tokens with the Admin SDK](https://firebase.google.com/docs/auth/admin/verify-id-tokens)
- [Firebase Authentication Admin and custom claims](https://firebase.google.com/docs/auth/admin)
- [Firestore Security Rules conditions](https://firebase.google.com/docs/firestore/security/rules-conditions)
