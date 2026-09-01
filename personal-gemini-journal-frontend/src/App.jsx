import { useCallback, useEffect, useState } from 'react';
import { authProvider } from './auth/authProvider';
import { api } from './api/client';
import { Header } from './components/layout/Header';
import { ViewTabs } from './components/layout/ViewTabs';
import { ErrorBanner } from './components/common/ErrorBanner';
import { PageLoader } from './components/common/PageLoader';
import { LoginScreen } from './components/auth/LoginScreen';
import { JournalView } from './components/journal/JournalView';
import { RagView } from './components/rag/RagView';
import { AccountabilityView } from './components/accountability/AccountabilityView';
import { WeeklyReflectionView } from './components/reflection/WeeklyReflectionView';
import { CalendarView } from './components/calendar/CalendarView';

export default function App() {
  const [user, setUser] = useState(null);
  const [authReady, setAuthReady] = useState(false);
  const [view, setView] = useState('journal');
  const [entries, setEntries] = useState([]);
  const [actionItems, setActionItems] = useState([]);
  const [ragMessages, setRagMessages] = useState([]);
  const [entryPage, setEntryPage] = useState({ nextCursor: null, hasMore: false });
  const [actionPage, setActionPage] = useState({ nextCursor: null, hasMore: false });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [identity, setIdentity] = useState({ roles: ['USER'] });

  const refreshEntries = useCallback(async () => {
    const page = await api('/api/journal/entries?limit=20');
    setEntries(page.items); setEntryPage(page);
    return page.items;
  }, []);

  const refreshActionItems = useCallback(async () => {
    for (let attempt = 0; attempt < 6; attempt += 1) {
      if (attempt > 0) await new Promise((resolve) => window.setTimeout(resolve, 2000));
      try {
        const page = await api('/api/action-items?limit=50');
        setActionItems(page.items); setActionPage(page); return page.items;
      } catch (requestError) {
        if (attempt === 5) setError(requestError.message);
      }
    }
  }, []);

  const loadData = useCallback(async (currentUser) => {
    if (!currentUser) return;
    setLoading(true);
    try {
      const [journalPage, goalPage, profile] = await Promise.all([
        api('/api/journal/entries?limit=20'),
        api('/api/action-items?limit=50'),
        api('/api/user/me'),
      ]);
      setEntries(journalPage.items); setEntryPage(journalPage);
      setActionItems(goalPage.items); setActionPage(goalPage);
      setIdentity(profile);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let unsubscribe = () => {};
    authProvider.initialize((nextUser) => { setUser(nextUser); setAuthReady(true); if (nextUser) loadData(nextUser); })
      .then((cleanup) => { unsubscribe = cleanup; }).catch((authError) => { setError(authError.message); setAuthReady(true); });
    return () => unsubscribe();
  }, [loadData]);

  useEffect(() => { setRagMessages([]); }, [user?.uid, user?.id, user?.sub]);

  if (!authReady) return <PageLoader fullScreen />;
  if (!user) return <LoginScreen initialError={error} />;

  return <div className="google-page-bg min-h-screen text-slate-800 selection:bg-[#D2E3FC]">
    <Header user={user} identity={identity} setError={setError} />
    <main className="mx-auto max-w-7xl px-6 py-8 lg:px-10 lg:py-10">
      <ViewTabs view={view} setView={setView} />
      {error && <ErrorBanner message={error} onClose={() => setError('')} />}
      {view === 'journal' && <JournalView entries={entries} setEntries={setEntries} items={actionItems} setItems={setActionItems} refreshEntries={refreshEntries} refreshItems={refreshActionItems} loading={loading} setError={setError} hasMore={entryPage.hasMore} loadMore={async () => { try { const page = await api(`/api/journal/entries?limit=20&cursor=${encodeURIComponent(entryPage.nextCursor)}`); setEntries((current) => [...current, ...page.items]); setEntryPage(page); } catch (requestError) { setError(requestError.message); } }} />}
      {view === 'rag' && <RagView messages={ragMessages} setMessages={setRagMessages} setError={setError} />}
      {view === 'goals' && <AccountabilityView items={actionItems} setItems={setActionItems} loading={loading} setError={setError} hasMore={actionPage.hasMore} loadMore={async () => { try { const page = await api(`/api/action-items?limit=50&cursor=${encodeURIComponent(actionPage.nextCursor)}`); setActionItems((current) => [...current, ...page.items]); setActionPage(page); } catch (requestError) { setError(requestError.message); } }} />}
      {view === 'weekly' && <WeeklyReflectionView setError={setError} />}
      {view === 'calendar' && <CalendarView setError={setError} />}
    </main>
  </div>;
}
