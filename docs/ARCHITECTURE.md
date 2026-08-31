# Architecture

## Architectural goals

The system is designed around four invariants:

1. The server, not the browser, establishes data ownership.
2. Journal content and model output remain isolated per authenticated subject.
3. AI and persistence providers can change without changing HTTP contracts.
4. Saving an entry must not silently lose its local accountability job.

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
3. Recent entries are loaded using the authenticated UID.
4. The AI adapter produces a bounded empathetic reflection.
5. The embedding adapter produces a vector.
6. The repository saves content, reflection, vector, and timestamp under the UID.
7. Local PostgreSQL inserts an accountability outbox row in the same transaction.
8. The response returns immediately; `extractedGoal` remains nullable because extraction is post-save.

## RAG flow

1. The query is validated and embedded.
2. `JournalRepository.findRelevant(uid, vector, 5)` performs UID-scoped retrieval.
3. PostgreSQL uses pgvector cosine distance; Firestore loads at most 100 documents from the UID path and ranks them in memory.
4. Only bounded matches become AI grounding context.
5. The prompt explicitly treats stored text as untrusted quoted data.
6. The response returns the AI answer and excerpts capped at 500 characters.

No global vector query exists. A user's query cannot retrieve another user's embedding or content.

## Local accountability outbox

```text
journal transaction
  |-- INSERT journal_entries
  `-- INSERT accountability_outbox(PENDING)

scheduled worker
  |-- reclaim stale PROCESSING jobs
  |-- claim with FOR UPDATE SKIP LOCKED
  |-- extract goals through Ollama
  |-- insert goals with unique(user, source entry, goal)
  `-- SUCCEEDED or exponential retry -> DEAD after max attempts
```

The outbox makes local extraction restart-safe. `locked_at` allows abandoned work to be reclaimed, retries are bounded, and the source-entry uniqueness constraint makes writes idempotent.

Cloud mode currently follows the hackathon requirement with a Spring `@Async` post-save task and three persistence attempts. Before a high-scale production launch, replace that best-effort task with Pub/Sub or Cloud Tasks and an idempotency key.

## Persistence model

### Local PostgreSQL

`journal_entries` stores `user_id`, content, AI response, `vector(768)`, creation time, and version. `action_items` stores owner, optional source entry, goal, state, and creation time. `accountability_outbox` is internal and has no HTTP endpoint.

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
