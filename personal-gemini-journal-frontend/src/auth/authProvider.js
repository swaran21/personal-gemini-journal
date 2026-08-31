const mode = import.meta.env.VITE_AUTH_MODE || 'oidc';
const providerPromise = mode === 'oidc' ? import('./keycloakProvider') : import('./firebaseProvider');

export const authProvider = {
  mode,
  loginLabel: mode === 'oidc' ? 'Sign in securely' : 'Sign in with Google',
  async initialize(listener) { return (await providerPromise).provider.initialize(listener); },
  async login() { return (await providerPromise).provider.login(); },
  async logout() { return (await providerPromise).provider.logout(); },
  async getAccessToken(forceRefresh = false) { return (await providerPromise).provider.getAccessToken(forceRefresh); },
};
