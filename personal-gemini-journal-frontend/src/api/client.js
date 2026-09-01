import { authProvider } from '../auth/authProvider';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:18080';

/** Sends an authenticated request and refreshes an expired OIDC/Firebase token once. */
async function authenticatedResponse(path, options = {}) {
  const request = async (forceRefresh = false) => {
    const token = await authProvider.getAccessToken(forceRefresh);
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
    await authProvider.logout();
    throw new Error('Your secure session has expired. Please sign in again.');
  }
  if (response.status === 429) {
    const retrySeconds = Number(response.headers.get('Retry-After'));
    const wait = Number.isFinite(retrySeconds) && retrySeconds > 0
      ? ` Try again in about ${Math.max(1, Math.ceil(retrySeconds / 60))} minute${retrySeconds > 60 ? 's' : ''}.`
      : ' Please try again later.';
    throw new Error(`You have reached the request limit.${wait}`);
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.error || `Request failed (${response.status})`);
  }
  return response;
}

export async function api(path, options = {}) {
  const response = await authenticatedResponse(path, options);
  return response.status === 204 ? null : response.json();
}

export async function download(path) {
  const response = await authenticatedResponse(path);
  const disposition = response.headers.get('Content-Disposition') || '';
  const filename = disposition.match(/filename="?([^";]+)"?/i)?.[1] || 'personal-gemini-journal-export';
  const url = URL.createObjectURL(await response.blob());
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
