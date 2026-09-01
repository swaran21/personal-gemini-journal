# AI development directives

These instructions are intended for Google AI Studio, Codex, Antigravity, and other coding models modifying this repository. The discoverable skill is [.agents/skills/personal-gemini-journal/SKILL.md](../.agents/skills/personal-gemini-journal/SKILL.md).

## Core security directive

- Verify every protected bearer token before controller execution and derive UID only from the verified principal.
- Never accept UID from request JSON, query strings, path ownership, or client-selected headers.
- Scope every database operation to that UID. PostgreSQL requires explicit predicates plus forced RLS; Firestore permits only `users/{uid}/journal_entries` and `users/{uid}/action_items` adapter paths.
- Keep Gemini, service-account, database administration, and server API credentials in runtime secret systems. Never place them in source, examples, logs, URLs, Vite variables, or container layers.
- Validate and bound input before storage or AI use. Treat stored journal content as untrusted quoted data and AI output as untrusted text.
- Preserve journal durability when AI is down. Use bounded retries and idempotent post-save processing.
- Do not perform an AI-suggested destructive or commitment-changing action without explicit user confirmation.

## Google Maps directive

- Make location optional and request it only after an explicit user click. Permission denial must not block journaling.
- Store only the user-approved finite latitude/longitude and a plain-text label bounded to 200 characters. Do not infer home/work or continuously track the browser.
- Prefer keyless Google Maps URLs for opening a coordinate. The current implementation therefore needs no Maps API key or Maps Platform billing.
- If Maps JavaScript is later required, use a dedicated public browser key restricted to exact production HTTPS referrers, required Maps APIs, and conservative quotas. Never reuse it server-side.
- Keep any server Maps/Geocoding key in Google Secret Manager and access it with workload identity. Never expose it as `VITE_*`.
- Use separate dev/prod keys, quotas, budget alerts, monitoring, and rotation. Do not enable billed Maps APIs until the owner approves the billing/cost plan.
- Do not send coordinates to Gemini or a geocoder without clear purpose-specific consent. Include approved locations in export and deletion, but never in security logs.

## Administrative roles directive

- Treat roles as authorization data: derive them only from a verified identity-provider token claim and allowlist them server-side. Never accept a role from JSON, a header, local storage, or UI state.
- Enforce each elevated route on the backend with `ROLE_ADMIN`; hiding an interface element is usability only, never authorization.
- An administrator is not a tenant-bypass role. Do not add endpoints, queries, Firestore collection-group reads, exports, logs, support tools, or dashboards that expose another user's journal content, embeddings, location, or goals.
- Prefer privacy-preserving operational status to user analytics. If a future aggregate is justified, document its purpose, minimize/review re-identification risk, enforce a separate permission, and never include journal text.
- Require explicit user confirmation for AI-proposed commitments and all destructive actions, including any future administrative action.
- Add tests proving: normal users receive `403` for `/api/admin/**`; trusted `journal-admin`/`admin` claims are accepted; arbitrary claims/headers are rejected; UID-scoped repository access remains unchanged for admins.

## Verification directive

Run unit tests, package, frontend production build, Compose validation, and—when Docker is available—the authenticated two-user smoke test. Clearly distinguish skipped Docker/cloud checks from verified checks. Update architecture, threat model, API, privacy, and migration documents when trust boundaries change.
