import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_OIDC_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_OIDC_REALM || 'journal',
  clientId: import.meta.env.VITE_OIDC_CLIENT_ID || 'journal-web',
});
let initialized = false;
let initialization;
let currentListener = null;

function user() {
  const claims = keycloak.tokenParsed || {};
  return {
    email: claims.email || claims.preferred_username || 'Local user',
    photoURL: claims.picture || null,
  };
}

export const provider = {
  async initialize(listener) {
    currentListener = listener;
    if (!initialized) {
      initialized = true;
      initialization = keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
        .catch((error) => { initialized = false; initialization = null; throw error; });
      await initialization;
      keycloak.onAuthSuccess = () => currentListener?.(user());
      keycloak.onAuthLogout = () => currentListener?.(null);
      keycloak.onTokenExpired = () => keycloak.updateToken(-1).catch(() => provider.logout());
    } else if (initialization) {
      await initialization;
    }
    listener(keycloak.authenticated ? user() : null);
    return () => { if (currentListener === listener) currentListener = null; };
  },
  async login() { await keycloak.login({ redirectUri: window.location.origin }); },
  async logout() { await keycloak.logout({ redirectUri: window.location.origin }); },
  async getAccessToken(forceRefresh = false) {
    if (!keycloak.authenticated) throw new Error('Your session has expired. Please sign in again.');
    await keycloak.updateToken(forceRefresh ? -1 : 30);
    return keycloak.token;
  },
};
