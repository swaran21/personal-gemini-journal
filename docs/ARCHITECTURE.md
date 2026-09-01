# Architecture

## Architectural goals

The system is designed around four invariants:

1. The server, not the browser, establishes data ownership.
2. Journal content and model output remain isolated per authenticated subject.
3. AI and persistence providers can change without changing HTTP contracts.
4. Journal durability must not depend on AI availability.
5. AI suggestions require user confirmation before becoming commitments.
6. Location collection is opt-in and data export omits internal AI machinery.

## Component view

```text
React application
  |-- authProvider
  |    |-- keycloakProvider (local)
  |    `-- firebaseProvider (cloud)
  `-- api client -> bearer token for every request
           |
Spring Security filter chain
  |-- OAuth2 resource server JWT decoder (local)
  `-- FirebaseAuthenticationFilter (cloud)
           |
Verified-UID rate limiter
           |
Controllers -> ChatService -> application ports
                            |-- GenerativeAiService
                            |-- EmbeddingService
                            |-- JournalRepository
                            `-- AccountabilityDispatcher
```

The controller always receives a `FirebasePrincipal`-shaped principal. In local mode its UID is the validated OIDC `sub`; in cloud mode it is the verified Firebase UID. This keeps the ownership contract identical without trusting a client-supplied identifier.

## Journal write flow

1. Bean Validation rejects missing, blank, or oversized content.
2. `ChatService` removes NUL characters and normalizes surrounding whitespace.
3. The repository atomically saves content with `PENDING` status and a local outbox job.
4. The API returns `202 Accepted`; no AI provider is on the request path.
5. A background worker loads recent entries using the authenticated owner stored in the trusted job.
6. The AI and embedding adapters generate reflection and vector; the entry becomes `COMPLETED`.
7. Optional goal extraction writes `PROPOSED` items. Its failure does not discard a successful reflection.
8. Bounded failures are retried; terminal failure leaves the journal readable with `FAILED` status.
9. `POST /api/journal/entries/{id}/retry` resets only an owned failed entry and reuses stored content.

## RAG flow

1. The question, IANA time zone, and at most 10 prior session turns are validated and sanitized.
2. Questions containing `today`, `yesterday`, `this/last week`, or `last/past/previous N hours` use exact UID-scoped SQL/date retrieval (`TEMPORAL_SQL`).
3. Location-intent questions prioritize bounded content/location-label matches and fuse them with vector matches (`LOCATION_HYBRID`).
4. Other questions fuse pgvector similarity with bounded lexical matches (`SEMANTIC_HYBRID`), deduplicated by owned entry ID.
5. If the embedding provider is rate-limited or unavailable, UID-scoped lexical retrieval remains available (`LEXICAL_SQL_FALLBACK`). Reflection persistence is independent of embedding success.
6. PostgreSQL uses pgvector cosine distance; Firestore loads at most 100 documents from the UID path and ranks them in memory.
5. Bounded entries, exact timestamps, current time, time zone, and bounded prior turns become the AI grounding prompt. Stored text and previous messages remain explicitly untrusted.
7. The response returns the AI answer, timestamped excerpts capped at 500 characters, and the retrieval mode.

No global vector query exists. A user's query cannot retrieve another user's embedding or content.

## Pagination, weekly reflection, and takeout

Journal and action-item lists use opaque URL-safe cursors encoding only the final row's creation time and document ID. PostgreSQL applies tuple keyset predicates; Firestore uses ordered `startAfter` queries below the UID collection. Limits are restricted to `1..100`; cursors never carry or select identity.

Weekly reflection resolves a client-provided IANA time zone, normalizes the requested date to Monday, and loads at most 100 owned entries from that seven-day interval. The current week stops at the current instant. It invokes AI only when entries exist, requires evidence-based highlights and a specific focus, and falls back only to quoted entry facts when a provider returns empty structured fields. Results are generated on demand and are not silently scheduled, which avoids hidden AI cost.

Data takeout streams JSON or Markdown while traversing UID-scoped pages. It exports journal text, reflection, processing state, approved location, and action items. Embeddings, outbox jobs, credentials, internal errors, and UID are excluded.

Location is provided by the browser only after a user gesture, validated again by the backend, and stored with the journal entry. The UI opens a keyless Google Maps URL rather than loading a third-party map SDK or browser API key.

The memory calendar resolves the requested year/month in the user's validated IANA time zone and queries only that authenticated UID's interval. It never trusts a browser UID and never loads a global calendar dataset.

## Authentication and roles

Local JWT roles come only from signed Keycloak `realm_access.roles`; cloud roles come only from verified Firebase custom claims. Every verified identity receives `ROLE_USER`. `journal-admin` or `admin` adds `ROLE_ADMIN`. `/api/admin/**` requires admin authority, while all ordinary `/api/**` routes require user authority. Roles control capabilities, not ownership: even administrators cannot read another user's journal through application repositories.

## Local accountability outbox

```text
journal transaction
  |-- INSERT journal_entries(PENDING)
  `-- INSERT accountability_outbox(PENDING)

scheduled worker
  |-- reclaim stale PROCESSING jobs
  |-- claim with FOR UPDATE SKIP LOCKED
  |-- reflect through Gemini Flash (recommended) or Ollama
  |-- embed through Gemini Embedding at 768 dimensions (recommended) or Ollama
  |-- UPDATE journal_entries(COMPLETED)
  |-- optionally extract PROPOSED goals
  |-- insert proposals with unique(user, source entry, goal)
  `-- SUCCEEDED or exponential retry -> DEAD after max attempts
```

The outbox makes local extraction restart-safe. `locked_at` allows abandoned work to be reclaimed, retries are bounded, and the source-entry uniqueness constraint makes writes idempotent.

Cloud mode currently follows the hackathon requirement with a Spring `@Async` post-save task and three persistence attempts. Before a high-scale production launch, replace that best-effort task with Pub/Sub or Cloud Tasks and an idempotency key.

## Persistence model

### Local PostgreSQL

`journal_entries` stores `user_id`, content, nullable AI response/vector, optional latitude/longitude/label, processing status/error, creation time, and version. `action_items` stores owner, optional source entry, goal, `PROPOSED/PENDING/COMPLETED` state, and creation time. AI output begins as `PROPOSED`; user-authored goals begin as `PENDING`. `accountability_outbox` is internal and has no HTTP endpoint.

## Load and privacy controls

The rate-limit filter runs only after authentication and keys quotas by verified principal UID. Local fixed-window state is bounded to 10,000 buckets. It protects the single-instance build without introducing Redis. Multi-instance Cloud Run requires a shared quota service or API gateway because instance-local counters are not globally authoritative.

Account deletion is data-first. PostgreSQL deletes owned action items and entries in one transaction, with entry deletion cascading to outbox jobs. Firestore deletes bounded batches only from both `users/{uid}` subcollections. Cloud then removes the Firebase identity; local Keycloak identity remains externally managed.

Forced RLS policies compare `user_id` to the transaction-local `app.current_user_id` setting. Repository methods set that value and also include explicit `WHERE user_id = :uid` predicates. The application login is not a superuser and cannot bypass RLS.

### Cloud Firestore

```text
users/{uid}/journal_entries/{entryId}
users/{uid}/action_items/{actionItemId}
```

The adapter creates those paths internally. It never performs an unpartitioned collection-group query and validates document IDs before updates or deletes.

## Deployment view

Local Compose exposes only loopback ports. The browser reaches Keycloak, the backend, and the frontend. Backend-to-backend traffic uses the Compose network and internal service names. PostgreSQL bootstrap credentials are available only to the database container; the backend receives only the non-superuser application credentials.

Cloud Run will replace the Compose network with managed services and workload identity. Cloud Run must terminate HTTPS, supply dynamic `PORT`, and run the `cloud` profile.
