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

1. The query is validated and embedded.
2. `JournalRepository.findRelevant(uid, vector, 5)` performs UID-scoped retrieval.
3. PostgreSQL uses pgvector cosine distance; Firestore loads at most 100 documents from the UID path and ranks them in memory.
4. Only bounded matches become AI grounding context.
5. The prompt explicitly treats stored text as untrusted quoted data.
6. The response returns the AI answer and excerpts capped at 500 characters.

No global vector query exists. A user's query cannot retrieve another user's embedding or content.

## Pagination, weekly reflection, and takeout

Journal and action-item lists use opaque URL-safe cursors encoding only the final row's creation time and document ID. PostgreSQL applies tuple keyset predicates; Firestore uses ordered `startAfter` queries below the UID collection. Limits are restricted to `1..100`; cursors never carry or select identity.

Weekly reflection resolves a client-provided IANA time zone, normalizes the requested date to Monday, loads at most 100 owned entries from that seven-day interval, and invokes AI only when entries exist. Results are generated on demand and are not silently scheduled, which avoids hidden AI cost.

Data takeout streams JSON or Markdown while traversing UID-scoped pages. It exports journal text, reflection, processing state, approved location, and action items. Embeddings, outbox jobs, credentials, internal errors, and UID are excluded.

Location is provided by the browser only after a user gesture, validated again by the backend, and stored with the journal entry. The UI opens a keyless Google Maps URL rather than loading a third-party map SDK or browser API key.

## Local accountability outbox

```text
journal transaction
  |-- INSERT journal_entries(PENDING)
  `-- INSERT accountability_outbox(PENDING)

scheduled worker
  |-- reclaim stale PROCESSING jobs
  |-- claim with FOR UPDATE SKIP LOCKED
  |-- reflect + embed through Ollama
  |-- UPDATE journal_entries(COMPLETED)
  |-- optionally extract PROPOSED goals
  |-- insert proposals with unique(user, source entry, goal)
  `-- SUCCEEDED or exponential retry -> DEAD after max attempts
```

The outbox makes local extraction restart-safe. `locked_at` allows abandoned work to be reclaimed, retries are bounded, and the source-entry uniqueness constraint makes writes idempotent.

Cloud mode currently follows the hackathon requirement with a Spring `@Async` post-save task and three persistence attempts. Before a high-scale production launch, replace that best-effort task with Pub/Sub or Cloud Tasks and an idempotency key.

## Persistence model

### Local PostgreSQL

`journal_entries` stores `user_id`, content, nullable AI response/vector, optional latitude/longitude/label, processing status/error, creation time, and version. `action_items` stores owner, optional source entry, goal, `PROPOSED/PENDING/COMPLETED` state, and creation time. `accountability_outbox` is internal and has no HTTP endpoint.

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
