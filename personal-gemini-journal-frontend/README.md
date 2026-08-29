# Personal Gemini Journal frontend

React 18, Vite, Tailwind CSS, Firebase Authentication, and Lucide icons for the Personal Gemini Journal API.

## Local setup

1. Copy `.env.example` to `.env.local`.
2. Set the Firebase Web App values, especially `VITE_FIREBASE_API_KEY`. These are browser configuration values; do not place Gemini or Google Cloud service-account secrets in this file.
3. In Firebase Console, enable Google as a sign-in provider and add `localhost` to the authorized domains.
4. Run `npm install` followed by `npm run dev`.

The Vite development server uses `http://localhost:3000`, matching the backend's development CORS default. The API URL defaults to `http://localhost:8080`; change `VITE_API_BASE_URL` only when the backend is hosted elsewhere.

Every API request retrieves an ID token from the signed-in Firebase user and sends it as `Authorization: Bearer <token>`.
