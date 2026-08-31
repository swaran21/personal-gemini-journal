# Security and threat model

## Protected assets

- Journal content, AI reflections, embeddings, and action items.
- Authentication tokens and account sessions.
- Gemini API key and Google Cloud service identity.
- Database credentials and Keycloak administration credentials.
- Availability and integrity of the AI/accountability workflows.

## Trust boundaries

The browser is untrusted. Request JSON, path variables, bearer tokens, historical journal text, and AI output are all treated as untrusted data. Keycloak/Firebase establish identity, Spring Security establishes the server principal, repositories enforce ownership, and the AI provider is an external processing boundary.

## Implemented controls

### Authentication

- Local tokens use Authorization Code flow with PKCE S256.
- Implicit and resource-owner-password flows are disabled in the committed realm.
- Spring validates issuer, JWK signature, time claims, required `journal-web` audience, and a bounded subject format.
- Cloud tokens use Firebase Admin verification with revoked-token checking.
- The API is stateless and accepts bearer tokens only in the `Authorization` header.
- The frontend obtains a current provider token for every fetch, performs one forced refresh after `401`, then signs out on `401/403`.
- Application code does not cache access tokens in `localStorage`.

### IDOR and tenant isolation

- Request DTOs contain no UID.
- Controllers read ownership only from `@AuthenticationPrincipal`.
- PostgreSQL statements use bound parameters and explicit `user_id = :uid` predicates.
- PostgreSQL forced RLS is a second ownership layer.
- The database application role is `NOSUPERUSER`, `NOCREATEDB`, and `NOCREATEROLE`.
- Firestore reads/writes use only the two `users/{verifiedUid}/...` paths.
- Unknown or cross-user action item IDs produce the same sanitized `404`.

### Input, prompt, and output handling

- Journal content is required and capped at 10,000 characters; RAG questions are capped at 4,000; goals are capped at 1,000.
- NUL characters are removed before persistence and model processing.
- SQL is parameterized; document IDs are allowlist validated.
- Prompts state that journal/history/context is untrusted quoted data and instructions inside it must be ignored.
- Structured AI JSON is parsed, deduplicated, count-limited, and length-limited.
- React renders response text through JSX, not `dangerouslySetInnerHTML`.
- RAG reference excerpts are capped at 500 characters.

### Secrets

- `.env.local`, service-account JSON, build output, and dependency directories are ignored.
- Local secrets are environment values and are not built into images.
- The backend never receives the PostgreSQL bootstrap password.
- Cloud uses Application Default Credentials/workload identity and Secret Manager.
- Gemini API keys are server-side only and sent in `x-goog-api-key`, not URL query strings.
- Vite variables are limited to public Firebase/OIDC/browser configuration; no Gemini or service-account secret is accepted by the frontend.

### Browser and transport

- CORS allows explicit local or production origins and only required methods/headers.
- Nginx sends CSP, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, a restrictive permissions policy, and a referrer policy.
- Development containers bind published ports to `127.0.0.1`.
- Cloud Run must enforce HTTPS; HSTS should be enabled only on the production HTTPS hostname.

### Error and operational safety

- Validation returns `400`, missing/invalid authentication returns `401`, ownership-safe missing resources return `404`, downstream unavailability returns `503`, and unexpected errors return sanitized `500` Problem Details.
- Responses never include stack traces, SQL, secret names, or downstream response bodies.
- AI jobs use bounded retries and do not log journal text.
- Frontend and backend run as non-root container users and have health checks.

## Abuse cases and expected result

| Abuse case | Expected control |
|---|---|
| Send another user's UID in JSON | DTO ignores/rejects it; verified principal remains authoritative |
| Guess another action-item UUID | UID predicate and RLS/path scoping return `404` |
| Forge or reuse an invalid JWT | Signature/issuer/audience/time validation returns `401` |
| Put prompt instructions in a journal | Content stays data; system prompt instructs model to ignore embedded instructions |
| Inject SQL in content | Bound JDBC parameters prevent SQL interpretation |
| Return HTML/script from AI | React text rendering and CSP prevent execution |
| Crash after saving an entry | Local outbox remains pending and is reclaimed |
| Crash after goal insert before completion mark | Unique source-entry/goal constraint makes retry idempotent |
| Expose a Gemini key in a browser bundle | No frontend configuration or code path accepts the key |

## Residual risks before public production

- Add gateway/load-balancer rate limits and per-user AI quotas to control abuse and cost.
- Replace the cloud `@Async` workflow with a durable managed queue.
- Add secret rotation procedures and validate rotation without restart if required.
- Configure centralized audit logging, alerts, retention, and redaction review.
- Run SAST, dependency/SBOM, container image, and DAST scans in CI.
- Perform a privacy review for journal retention/export/deletion and obtain user consent for model processing.
- Decide whether application-layer encryption is required in addition to managed encryption at rest.
- Add production Firebase App Check only as defense in depth; it does not replace authentication.
- Test Firestore rules in the emulator and in a dedicated test project before launch.
- Pin production images by digest and define a patch/update policy.

Security is a maintained property, not a one-time claim. Re-run the threat model whenever a new integration, role, notification channel, or external API is added.
