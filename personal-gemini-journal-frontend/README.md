# Personal Gemini Journal Frontend

React 18/Vite client for daily reflection, private RAG, weekly insights, accountability, location pins, and privacy takeout. The component structure preserves the original warm off-white/sage journal design while separating authentication, API access, layout, journal, RAG, reflection, goal, and account features.

## Stack

- React 18.
- Vite 8.
- Tailwind CSS 3.
- Lucide React icons.
- Keycloak JS for local OIDC Authorization Code + PKCE.
- Firebase Web SDK for cloud Google Sign-In.
- Unprivileged Nginx production container.

## Source structure

```text
src/
|-- api/client.js                 Authenticated fetch/refresh/error policy
|-- auth/
|   |-- authProvider.js           Profile selection
|   |-- keycloakProvider.js       Local OIDC provider
|   `-- firebaseProvider.js       Cloud Firebase provider
|-- components/
|   |-- auth/                     Login guard
|   |-- layout/                   Header and view tabs
|   |-- journal/                  Composer and entry feed
|   |-- rag/                      Past-self conversation and references
|   |-- reflection/               On-demand weekly insight view
|   |-- accountability/           Dashboard/progress
|   |-- actions/                  Reusable goal controls
|   |-- account/                  Destructive deletion confirmation
|   `-- common/                   Loader, error, and empty states
|-- App.jsx                       Authenticated state and view orchestration
|-- firebase.js                   Lazily loaded public Firebase config
`-- styles.css                    Tailwind/global design tokens
```

## Authentication behavior

`VITE_AUTH_MODE=oidc` dynamically loads Keycloak. Tokens remain inside the provider instance, Authorization Code flow uses PKCE S256, silent iframe checking is disabled, and expiry triggers refresh/logout handling.

`VITE_AUTH_MODE=firebase` dynamically loads Firebase only in cloud builds. Every API call obtains `auth.currentUser.getIdToken(forceRefresh)`; no token is stored in React state or local storage by application code.

The shared API client:

1. Retrieves a current access token for every request.
2. Adds `Authorization: Bearer <token>`.
3. Adds JSON content type only when a body exists.
4. Retries once with forced refresh after `401`.
5. Signs out and returns a visible session error after persistent `401/403`.
6. Parses sanitized backend Problem Details.

The frontend never sends a UID.

## Views

### Daily Journal

The textarea is capped at 10,000 characters and preserves the draft after failure. Submission saves immediately; the new entry renders with a background-processing indicator while the feed polls boundedly for completion. A provider failure never removes the journal text and exposes a safe retry control using the stored entry ID rather than resending content from the browser. The feed uses opaque cursor pagination.

Location is optional. Browser geolocation runs only after **Add current location**, permission denial does not block saving, and the approved pin can be opened through a keyless Google Maps URL. No Maps key is embedded in the application.

### Chat with Past Self

Queries `/api/chat/rag`, renders a conversational response, and exposes bounded `referencedEntries` through a collapsible context list. Model content is rendered as text, not HTML.

### Accountability Dashboard

Loads cursor pages from `/api/action-items`. AI output is rendered first as a proposal with **Add goal** and **Dismiss** controls. Completion percentage includes only loaded accepted goals. Status changes and deletion are optimistic and roll back on failure.

### Weekly Reflection

Sends the browser's IANA time-zone name to `/api/reflections/weekly` and renders grounded highlights, accomplishments, unresolved themes, and one suggested focus. Generation is user-triggered, shows the number of source entries, and has a no-entry state.

### Privacy and deletion

The shield control opens a destructive confirmation dialog. The user must type `DELETE`; the request sends no UID and uses the same freshly acquired bearer token. In local OIDC mode the dialog explains that application data is removed while the externally managed Keycloak login record remains. Cloud mode also deletes the Firebase identity.

The download control offers JSON and Markdown takeout. The authenticated response is saved as a browser download; tokens are not placed in URLs and object URLs are immediately revoked.

## Environment

Copy `.env.example` to `.env.local` for Vite development. Local defaults are:

```text
VITE_API_BASE_URL=http://localhost:18080
VITE_AUTH_MODE=oidc
VITE_OIDC_URL=http://localhost:8180
VITE_OIDC_REALM=journal
VITE_OIDC_CLIENT_ID=journal-web
```

Firebase variables are required only for cloud mode. They are public browser-app identifiers, not server credentials. Never create a `VITE_GEMINI_API_KEY`, `VITE_SERVICE_ACCOUNT`, or other private `VITE_*` setting because Vite embeds it in downloadable JavaScript.

## Run and verify

```powershell
npm ci
npm audit
npm run dev
npm run build
```

Vite serves on port `3000`; the Dockerized unprivileged Nginx frontend is exposed by the root Compose file on `13000`.

## Browser security

The production container sends a restrictive Content Security Policy, denies framing, prevents MIME sniffing, limits referrer data, disables camera/microphone, and allows geolocation only to the same origin after browser permission. It binds to a non-privileged internal port and runs as UID `101`.

Cloud deployment must rebuild CSP/connect targets for the HTTPS frontend/backend/Firebase origins and should add HSTS only on the final HTTPS hostname.
