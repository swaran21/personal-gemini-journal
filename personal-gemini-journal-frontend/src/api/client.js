import { signOut } from 'firebase/auth';
import { auth } from '../firebase';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/** Sends an authenticated request and refreshes an expired Firebase token once. */
export async function api(path, options = {}) {
  const currentUser = auth.currentUser;
  if (!currentUser) throw new Error('Your session has expired. Please sign in again.');

  const request = async (forceRefresh = false) => {
    const token = await currentUser.getIdToken(forceRefresh);
    return fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
    });
  };

  let response = await request();
  if (response.status === 401) response = await request(true);
  if (response.status === 401 || response.status === 403) {
    await signOut(auth);
    throw new Error('Your secure session has expired. Please sign in again.');
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.error || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
}
