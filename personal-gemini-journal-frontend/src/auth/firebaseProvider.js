import { onAuthStateChanged, signInWithPopup, signOut } from 'firebase/auth';
import { auth, googleProvider } from '../firebase';

export const provider = {
  async initialize(listener) { return onAuthStateChanged(auth, listener); },
  async login() { await signInWithPopup(auth, googleProvider); },
  async logout() { await signOut(auth); },
  async getAccessToken(forceRefresh = false) {
    if (!auth.currentUser) throw new Error('Your session has expired. Please sign in again.');
    return auth.currentUser.getIdToken(forceRefresh);
  },
};
