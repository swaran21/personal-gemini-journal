---
name: personal-gemini-journal
description: Securely inspect, extend, test, or deploy the Personal Gemini Journal Spring Boot and React monorepo. Use for journal, RAG, weekly reflection, accountability, location, export/deletion, authentication, PostgreSQL/pgvector, Firestore, Ollama/Gemini, Docker, Firebase, Google Maps, Cloud Run, API-contract, privacy, and security work in this repository.
---

# Personal Gemini Journal

Preserve the product's privacy and local/cloud adapter boundaries while making focused, verified changes.

## Start every task

1. Read `references/project-contract.md` for structure, API ownership, profiles, and verification commands.
2. Read `references/google-maps-security.md` before changing location or Maps behavior.
3. Inspect `git status --short`; preserve unrelated user changes.
4. Treat backend contracts and verified-principal ownership as authoritative.

## Security invariants

- Derive UID only from the verified `FirebasePrincipal`; never accept UID in a DTO, query, or trusted header.
- Scope PostgreSQL by explicit `user_id` predicates plus forced RLS; scope Firestore below `users/{uid}` with no collection-group queries.
- Keep Gemini, service-account, database-admin, and server Maps credentials outside source and browser bundles.
- Treat journal, location labels, stored memories, and AI output as untrusted data. Validate length/range and never render AI HTML.
- Keep journal persistence independent from AI availability and retain bounded retries/idempotency.
- Export only user-visible data; omit embeddings, outbox records, credentials, and internal errors.
- Make location opt-in. Never infer, continuously track, or silently enrich coordinates.

## Change workflow

1. Trace the request across React API call, controller DTO, service, repository port, both persistence adapters, migration, and tests.
2. Change the smallest coherent slice in both local and cloud adapters.
3. Add unit tests for validation, principal scoping, empty/error states, and page boundaries. Add Testcontainers coverage for SQL/RLS changes.
4. Run backend tests and package, frontend production build, and Compose validation. Run the authenticated smoke when Docker is available.
5. Update root/backend/frontend/security/architecture documentation when contracts or trust boundaries change.
6. Split commits by feature and inspect staged paths before each commit.

## Stop rules

- Do not claim Firestore, Firebase, Gemini, Maps Platform, or Cloud Run is verified without live credentials and evidence.
- Do not weaken CORS, JWT validation, RLS, path isolation, rate limits, or CSP to make a test pass.
- Do not add a frontend secret or direct browser database write.
- Stop and request direction before production IAM, billing, deletion outside the authenticated user's scope, or other external-state changes.
