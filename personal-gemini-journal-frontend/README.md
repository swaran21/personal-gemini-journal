# Personal Gemini Journal Frontend

React/Vite client for private journaling, Gemini reflection, “Chat with Past Self,” and accountability tracking. The UI preserves the original calm visual language: warm off-white surfaces, sage actions, rounded cards, journal bubbles, and an Extracted Insights panel.

## Features

### Authentication

Unauthenticated visitors see a Google sign-in card. Firebase Authentication handles the sign-in flow. Authenticated users see their avatar/email, a secure-session indicator, and sign-out control.

### Daily Journal

The composer accepts up to 10,000 characters and sends `{ "content": "..." }` to the backend. A loading state displays “Gemini is reflecting...” while the entry is processed. The feed renders the user's text, Gemini's response, timestamps, and an extracted-goal badge when available. The original-style sidebar lists action items.

### Chat with Past Self

The RAG view sends `{ "query": "..." }` to `/api/chat/rag`. User messages and grounded responses appear in a conversation. Bounded excerpts from the referenced private memories are hidden behind a collapsible “Referenced memories” control.

### Accountability Dashboard

The dashboard calculates completion percentage, displays a progress bar, groups action items by status, and supports optimistic checkbox updates. Failed requests roll back the UI and show a dismissible error. Items can also be deleted.

## Source structure

```text
src/
├── App.jsx       Auth guard, views, API calls, and application state
├── firebase.js   Firebase Web SDK configuration
├── main.jsx      React entry point
└── styles.css    Tailwind directives and global light theme
```

## Requirements

- Node.js 18 or newer
- npm
- Firebase Web App with Google provider enabled
- `localhost` in Firebase Authentication authorized domains
- Spring Boot backend running on port 8080, or a configured API origin

## Environment setup

Copy `.env.example` to `.env.local` and fill in the Firebase Web App values:

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Backend origin; defaults to `http://localhost:8080` |
| `VITE_FIREBASE_API_KEY` | Firebase browser API key |
| `VITE_FIREBASE_AUTH_DOMAIN` | Firebase Auth domain |
| `VITE_FIREBASE_PROJECT_ID` | Firebase project identifier |
| `VITE_FIREBASE_STORAGE_BUCKET` | Firebase storage bucket |
| `VITE_FIREBASE_MESSAGING_SENDER_ID` | Firebase messaging sender ID |
| `VITE_FIREBASE_APP_ID` | Firebase Web App ID |

Firebase Web configuration identifies the browser application, but it is not a replacement for server credentials. Never add Gemini keys, service-account JSON, or Secret Manager credentials to `.env.local`; Vite embeds `VITE_*` values into browser assets.

## Install and run

Run `npm ci` for a clean lockfile installation, then `npm run dev`. Vite serves the application at `http://localhost:3000`, matching the backend development CORS default. Use `npm run build` for a production bundle and `npm run preview` to serve that bundle locally.

## API and token behavior

Before every fetch, the client calls `user.getIdToken()` and sends `Authorization: Bearer <token>`. It never sends a UID; the backend derives ownership from the verified token.

| UI action | Request |
|---|---|
| Save entry | `POST /api/journal/entry` with `{content}` |
| Load entries | `GET /api/journal/entries` |
| Ask past self | `POST /api/chat/rag` with `{query}` |
| Load goals | `GET /api/action-items` |
| Toggle goal | `PATCH /api/action-items/{id}` with `{status}` |
| Delete goal | `DELETE /api/action-items/{id}` |

The client renders API values as text, formats ISO-8601 timestamps with the browser locale, limits input lengths, and turns request failures into visible error banners.

## Responsive behavior

On large screens, the journal feed and composer occupy the primary column while Extracted Insights stays sticky on the right. On small screens the layout becomes one column, tabs scroll horizontally, profile details condense, and controls remain keyboard accessible.

## Troubleshooting

- **Missing Firebase configuration:** copy `.env.example` to `.env.local` and fill every required value. `src/firebase.js` reports missing names early.
- **401 Unauthorized:** confirm the frontend and backend use the same Firebase project and sign in again.
- **CORS failure:** use port 3000 locally or configure backend `CORS_ALLOWED_ORIGINS` for the deployed frontend origin.
- **Network failure:** start the backend on port 8080 or set `VITE_API_BASE_URL`.
- **Goals appear later:** extraction runs asynchronously after an entry is saved; revisit the Accountability tab after Gemini completes.
