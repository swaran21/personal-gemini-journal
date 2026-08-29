import { useCallback, useEffect, useState } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from './firebase';
import { api } from './api/client';
import { Header } from './components/layout/Header';
import { ViewTabs } from './components/layout/ViewTabs';
import { ErrorBanner } from './components/common/ErrorBanner';
import { PageLoader } from './components/common/PageLoader';
import { LoginScreen } from './components/auth/LoginScreen';
import { JournalView } from './components/journal/JournalView';
import { RagView } from './components/rag/RagView';
import { AccountabilityView } from './components/accountability/AccountabilityView';

export default function App() {
  const [user, setUser] = useState(null);
  const [authReady, setAuthReady] = useState(false);
  const [view, setView] = useState('journal');
  const [entries, setEntries] = useState([]);
  const [actionItems, setActionItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadData = useCallback(async (currentUser) => {
    if (!currentUser) return;
    setLoading(true);
    try {
      const [journalEntries, goals] = await Promise.all([
        api('/api/journal/entries'),
        api('/api/action-items'),
      ]);
      setEntries(journalEntries);
      setActionItems(goals);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => onAuthStateChanged(auth, (nextUser) => {
    setUser(nextUser);
    setAuthReady(true);
    if (nextUser) loadData(nextUser);
  }), [loadData]);

  if (!authReady) return <PageLoader fullScreen />;
  if (!user) return <LoginScreen />;

  return <div className="min-h-screen bg-[#F9F8F4] text-[#2D362E] selection:bg-[#7A8D80]/20">
    <Header user={user} />
    <main className="mx-auto max-w-7xl px-6 py-8 lg:px-10 lg:py-10">
      <ViewTabs view={view} setView={setView} />
      {error && <ErrorBanner message={error} onClose={() => setError('')} />}
      {view === 'journal' && <JournalView entries={entries} setEntries={setEntries} items={actionItems} setItems={setActionItems} loading={loading} setError={setError} />}
      {view === 'rag' && <RagView setError={setError} />}
      {view === 'goals' && <AccountabilityView items={actionItems} setItems={setActionItems} loading={loading} setError={setError} />}
    </main>
  </div>;
}
