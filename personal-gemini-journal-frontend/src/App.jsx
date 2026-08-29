import { useCallback, useEffect, useMemo, useState } from 'react';
import { onAuthStateChanged, signInWithPopup, signOut } from 'firebase/auth';
import {
  BookHeart, Bot, CheckCircle2, ChevronDown, ChevronUp, Circle, Compass,
  Loader2, LogIn, LogOut, MessageCircleHeart, Send, Sparkles, Target, X,
} from 'lucide-react';
import { auth, googleProvider } from './firebase';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function api(user, path, options = {}) {
  const token = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.error || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
}

function formatDate(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Just now' : new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }).format(date);
}

function App() {
  const [user, setUser] = useState(null);
  const [authReady, setAuthReady] = useState(false);
  const [tab, setTab] = useState('journal');
  const [entries, setEntries] = useState([]);
  const [actionItems, setActionItems] = useState([]);
  const [loadingData, setLoadingData] = useState(false);
  const [error, setError] = useState('');

  const loadData = useCallback(async (currentUser) => {
    if (!currentUser) return;
    setLoadingData(true);
    try {
      const [journalEntries, goals] = await Promise.all([
        api(currentUser, '/api/journal/entries'),
        api(currentUser, '/api/action-items'),
      ]);
      setEntries(journalEntries);
      setActionItems(goals);
    } catch (requestError) { setError(requestError.message); }
    finally { setLoadingData(false); }
  }, []);

  useEffect(() => onAuthStateChanged(auth, (nextUser) => { setUser(nextUser); setAuthReady(true); if (nextUser) loadData(nextUser); }), [loadData]);

  const dismissError = () => setError('');
  if (!authReady) return <ScreenLoader />;
  if (!user) return <LoginScreen />;

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <header className="sticky top-0 z-30 border-b border-white/10 bg-zinc-950/85 backdrop-blur-xl">
        <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-5 sm:px-8">
          <div className="flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-500 shadow-lg shadow-indigo-500/20"><BookHeart className="h-5 w-5" /></div><div><h1 className="font-serif text-lg font-semibold tracking-tight">Gemini Journal</h1><p className="text-xs text-zinc-500">A private space to reflect</p></div></div>
          <div className="flex items-center gap-3"><div className="hidden items-center gap-2 text-right sm:flex"><div><p className="max-w-40 truncate text-sm text-zinc-300">{user.email}</p><p className="text-xs text-emerald-400">Secure session</p></div>{user.photoURL ? <img className="h-9 w-9 rounded-full border border-white/10" src={user.photoURL} alt="Your profile" referrerPolicy="no-referrer" /> : <div className="grid h-9 w-9 place-items-center rounded-full bg-zinc-800 text-sm">{user.email?.[0]?.toUpperCase()}</div>}</div><button onClick={() => signOut(auth)} className="rounded-lg p-2 text-zinc-400 transition hover:bg-zinc-800 hover:text-white" aria-label="Sign out"><LogOut className="h-5 w-5" /></button></div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-5 py-8 sm:px-8 sm:py-10">
        <nav className="mb-8 grid grid-cols-3 rounded-2xl border border-white/10 bg-zinc-900/70 p-1.5" aria-label="Journal views">
          <TabButton active={tab === 'journal'} onClick={() => setTab('journal')} icon={<BookHeart className="h-4 w-4" />} label="Daily Journal" />
          <TabButton active={tab === 'rag'} onClick={() => setTab('rag')} icon={<MessageCircleHeart className="h-4 w-4" />} label="Past Self" />
          <TabButton active={tab === 'goals'} onClick={() => setTab('goals')} icon={<Target className="h-4 w-4" />} label="Accountability" />
        </nav>

        {error && <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl border border-rose-400/25 bg-rose-500/10 p-4 text-sm text-rose-100"><X className="mt-0.5 h-4 w-4 shrink-0" /><p className="flex-1">{error}</p><button onClick={dismissError} className="text-rose-200 hover:text-white" aria-label="Dismiss error">×</button></div>}
        {tab === 'journal' && <JournalView user={user} entries={entries} setEntries={setEntries} setError={setError} />}
        {tab === 'rag' && <RagView user={user} setError={setError} />}
        {tab === 'goals' && <GoalsView user={user} actionItems={actionItems} setActionItems={setActionItems} loading={loadingData} setError={setError} />}
      </main>
    </div>
  );
}

function LoginScreen() {
  const [signingIn, setSigningIn] = useState(false); const [error, setError] = useState('');
  const login = async () => { setSigningIn(true); setError(''); try { await signInWithPopup(auth, googleProvider); } catch (signInError) { setError(signInError.message.replace('Firebase: ', '')); } finally { setSigningIn(false); } };
  return <div className="grid min-h-screen place-items-center bg-zinc-950 p-5"><section className="w-full max-w-md rounded-3xl border border-white/10 bg-zinc-900/70 p-8 text-center shadow-2xl shadow-black/30"><div className="mx-auto mb-6 grid h-16 w-16 place-items-center rounded-2xl bg-indigo-500 shadow-lg shadow-indigo-500/20"><BookHeart className="h-8 w-8" /></div><p className="mb-3 text-xs font-bold uppercase tracking-[.24em] text-indigo-300">Personal Gemini Journal</p><h1 className="font-serif text-3xl font-semibold tracking-tight">A calmer place to think.</h1><p className="mt-4 leading-relaxed text-zinc-400">Reflect privately, revisit the moments that matter, and turn intentions into gentle commitments.</p>{error && <p className="mt-5 rounded-lg bg-rose-500/10 p-3 text-sm text-rose-200">{error}</p>}<button onClick={login} disabled={signingIn} className="mt-8 flex w-full items-center justify-center gap-3 rounded-xl bg-indigo-500 px-5 py-3.5 font-medium text-white transition hover:bg-indigo-400 disabled:cursor-not-allowed disabled:opacity-60">{signingIn ? <Loader2 className="h-5 w-5 animate-spin" /> : <LogIn className="h-5 w-5" />}{signingIn ? 'Signing in…' : 'Sign in with Google'}</button></section></div>;
}

function JournalView({ user, entries, setEntries, setError }) {
  const [content, setContent] = useState(''); const [submitting, setSubmitting] = useState(false);
  const submit = async (event) => { event.preventDefault(); const value = content.trim(); if (!value || submitting) return; setSubmitting(true); setError(''); try { const created = await api(user, '/api/journal/entry', { method: 'POST', body: JSON.stringify({ content: value }) }); setEntries((items) => [created, ...items]); setContent(''); } catch (requestError) { setError(requestError.message); } finally { setSubmitting(false); } };
  return <div className="mx-auto max-w-4xl"><section className="rounded-3xl border border-white/10 bg-gradient-to-br from-indigo-500/15 via-zinc-900 to-zinc-900 p-6 shadow-xl shadow-black/15 sm:p-8"><div className="mb-5 flex items-center gap-2 text-sm font-semibold uppercase tracking-[.16em] text-indigo-200"><Sparkles className="h-4 w-4" /> What&apos;s on your mind?</div><form onSubmit={submit}><textarea value={content} onChange={(event) => setContent(event.target.value)} maxLength={10000} className="min-h-44 w-full resize-y rounded-2xl border border-white/10 bg-zinc-950/70 p-5 leading-relaxed text-zinc-100 placeholder:text-zinc-600" placeholder="Write freely. Gemini will reflect with you and identify commitments when they are present." /><div className="mt-4 flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between"><p className="text-xs text-zinc-500">Your words are processed through your authenticated private journal.</p><button disabled={submitting || !content.trim()} className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-500 px-5 py-3 text-sm font-semibold transition hover:bg-indigo-400 disabled:cursor-not-allowed disabled:opacity-50">{submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}{submitting ? 'Gemini is reflecting…' : 'Save reflection'}</button></div></form></section><section className="mt-10"><div className="mb-5 flex items-center justify-between"><h2 className="text-sm font-bold uppercase tracking-[.18em] text-zinc-400">Past reflections</h2><span className="rounded-full border border-white/10 px-3 py-1 text-xs text-zinc-500">{entries.length} memories</span></div>{entries.length ? <div className="space-y-7">{entries.map((entry) => <EntryCard key={entry.id} entry={entry} />)}</div> : <EmptyState icon={<Compass />} title="No memories logged yet" description="Your reflections will appear here as a private conversation with yourself." />}</section></div>;
}

function EntryCard({ entry }) { return <article className="space-y-4"><p className="text-center text-xs text-zinc-600">{formatDate(entry.createdAt)}</p><div className="ml-auto max-w-[88%] rounded-2xl rounded-tr-sm bg-indigo-500 px-5 py-4 text-sm leading-relaxed text-white shadow-lg shadow-indigo-950/20 sm:max-w-[78%] whitespace-pre-wrap">{entry.content}</div><div className="max-w-[88%] rounded-2xl rounded-tl-sm border border-white/10 bg-zinc-900 px-5 py-4 text-sm leading-relaxed text-zinc-300 sm:max-w-[78%] whitespace-pre-wrap"><div className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase tracking-[.16em] text-indigo-300"><Bot className="h-3.5 w-3.5" /> Gemini reflection</div>{entry.aiResponse}</div>{entry.extractedGoal && <div className="flex justify-end"><span className="inline-flex max-w-[88%] items-center gap-2 rounded-full border border-amber-300/20 bg-amber-300/10 px-3 py-1.5 text-xs text-amber-100"><Target className="h-3.5 w-3.5 shrink-0" />{entry.extractedGoal}</span></div>}</article>; }

function RagView({ user, setError }) {
  const [query, setQuery] = useState(''); const [messages, setMessages] = useState([]); const [asking, setAsking] = useState(false);
  const ask = async (event) => { event.preventDefault(); const value = query.trim(); if (!value || asking) return; setAsking(true); setError(''); setMessages((items) => [...items, { role: 'user', text: value }]); setQuery(''); try { const result = await api(user, '/api/chat/rag', { method: 'POST', body: JSON.stringify({ query: value }) }); setMessages((items) => [...items, { role: 'assistant', ...result }]); } catch (requestError) { setError(requestError.message); setMessages((items) => items.slice(0, -1)); } finally { setAsking(false); } };
  return <div className="mx-auto flex min-h-[580px] max-w-4xl flex-col rounded-3xl border border-white/10 bg-zinc-900/65"><div className="border-b border-white/10 px-6 py-5"><div className="flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-500/15 text-indigo-300"><MessageCircleHeart className="h-5 w-5" /></div><div><h2 className="font-serif text-xl font-semibold">Chat with Past Self</h2><p className="text-sm text-zinc-500">Answers grounded in your own memories.</p></div></div></div><div className="flex-1 space-y-5 overflow-y-auto p-5 sm:p-7">{!messages.length && <EmptyState icon={<MessageCircleHeart />} title="Ask your past self" description="Try “When did I feel most productive last week?” or “What helped me stay consistent?”" />}{messages.map((message, index) => message.role === 'user' ? <div key={index} className="ml-auto max-w-[85%] rounded-2xl rounded-tr-sm bg-indigo-500 px-5 py-3.5 text-sm leading-relaxed text-white">{message.text}</div> : <RagReply key={index} message={message} />)}{asking && <div className="flex items-center gap-2 text-sm text-zinc-500"><Loader2 className="h-4 w-4 animate-spin" /> Searching your memories…</div>}</div><form onSubmit={ask} className="border-t border-white/10 p-4 sm:p-5"><div className="flex gap-3"><input value={query} onChange={(event) => setQuery(event.target.value)} maxLength={4000} className="min-w-0 flex-1 rounded-xl border border-white/10 bg-zinc-950 px-4 py-3 text-sm placeholder:text-zinc-600" placeholder="Ask about a memory, habit, or feeling…" /><button disabled={asking || !query.trim()} className="grid h-11 w-11 place-items-center rounded-xl bg-indigo-500 transition hover:bg-indigo-400 disabled:cursor-not-allowed disabled:opacity-50" aria-label="Ask past self"><Send className="h-4 w-4" /></button></div></form></div>;
}

function RagReply({ message }) { const [open, setOpen] = useState(false); return <div className="max-w-[92%] rounded-2xl rounded-tl-sm border border-white/10 bg-zinc-950/70 p-5 text-sm leading-relaxed text-zinc-300"><div className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase tracking-[.16em] text-indigo-300"><Bot className="h-3.5 w-3.5" /> Past self</div><p className="whitespace-pre-wrap">{message.reply}</p>{message.referencedEntries?.length > 0 && <div className="mt-4 border-t border-white/10 pt-3"><button onClick={() => setOpen(!open)} className="flex w-full items-center justify-between text-xs font-medium text-zinc-400 hover:text-white">Referenced memories <span className="flex items-center gap-1">{message.referencedEntries.length}{open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}</span></button>{open && <ul className="mt-3 space-y-2">{message.referencedEntries.map((reference) => <li key={reference} className="rounded-lg bg-zinc-900 px-3 py-2 text-xs text-zinc-500">Memory {reference}</li>)}</ul>}</div>}</div>; }

function GoalsView({ user, actionItems, setActionItems, loading, setError }) {
  const [updating, setUpdating] = useState(new Set()); const completed = actionItems.filter((item) => item.status === 'COMPLETED').length; const percentage = actionItems.length ? Math.round((completed / actionItems.length) * 100) : 0;
  const toggle = async (item) => { if (updating.has(item.id)) return; const previous = item.status; const status = previous === 'COMPLETED' ? 'PENDING' : 'COMPLETED'; setUpdating((ids) => new Set(ids).add(item.id)); setActionItems((items) => items.map((value) => value.id === item.id ? { ...value, status } : value)); try { await api(user, `/api/action-items/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status }) }); } catch (requestError) { setActionItems((items) => items.map((value) => value.id === item.id ? { ...value, status: previous } : value)); setError(requestError.message); } finally { setUpdating((ids) => { const next = new Set(ids); next.delete(item.id); return next; }); } };
  const pending = actionItems.filter((item) => item.status === 'PENDING'); const done = actionItems.filter((item) => item.status === 'COMPLETED');
  return <div className="mx-auto max-w-6xl"><section className="mb-8 rounded-3xl border border-white/10 bg-zinc-900/70 p-6 sm:p-8"><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-bold uppercase tracking-[.18em] text-indigo-300">Accountability dashboard</p><h2 className="mt-2 font-serif text-3xl font-semibold">Small promises, visible progress.</h2><p className="mt-2 text-sm text-zinc-500">Gemini extracts commitments after each reflection.</p></div><div className="text-left sm:text-right"><p className="text-3xl font-semibold text-white">{percentage}%</p><p className="text-sm text-zinc-500">{completed} of {actionItems.length} complete</p></div></div><div className="mt-6 h-2 overflow-hidden rounded-full bg-zinc-800"><div className="h-full rounded-full bg-indigo-500 transition-all" style={{ width: `${percentage}%` }} /></div></section>{loading ? <ScreenLoader /> : actionItems.length ? <div className="grid gap-6 lg:grid-cols-2"><GoalColumn title="Pending" count={pending.length} accent="amber" items={pending} toggle={toggle} updating={updating} /><GoalColumn title="Completed" count={done.length} accent="emerald" items={done} toggle={toggle} updating={updating} /></div> : <EmptyState icon={<Target />} title="No commitments yet" description="When you write a concrete goal or deadline, it will appear here automatically." />}</div>;
}

function GoalColumn({ title, count, accent, items, toggle, updating }) { const accentClass = accent === 'amber' ? 'text-amber-300 border-amber-300/20 bg-amber-300/10' : 'text-emerald-300 border-emerald-300/20 bg-emerald-300/10'; return <section className="rounded-2xl border border-white/10 bg-zinc-900/60 p-5"><div className="mb-4 flex items-center justify-between"><h3 className="text-sm font-bold uppercase tracking-[.16em] text-zinc-400">{title}</h3><span className={`rounded-full border px-2.5 py-1 text-xs ${accentClass}`}>{count}</span></div><div className="space-y-3">{items.length ? items.map((item) => <button key={item.id} onClick={() => toggle(item)} className="group flex w-full items-start gap-3 rounded-xl border border-white/10 bg-zinc-950/65 p-4 text-left transition hover:border-indigo-400/40"><span className="mt-0.5 text-zinc-500 group-hover:text-indigo-300">{updating.has(item.id) ? <Loader2 className="h-5 w-5 animate-spin" /> : item.status === 'COMPLETED' ? <CheckCircle2 className="h-5 w-5 text-emerald-400" /> : <Circle className="h-5 w-5" />}</span><span className="flex-1"><span className={`block text-sm leading-relaxed ${item.status === 'COMPLETED' ? 'text-zinc-500 line-through' : 'text-zinc-200'}`}>{item.goal}</span><span className="mt-2 block text-xs text-zinc-600">{formatDate(item.createdAt)}</span></span></button>) : <p className="rounded-xl border border-dashed border-white/10 px-4 py-7 text-center text-sm text-zinc-600">Nothing here yet.</p>}</div></section>; }

function TabButton({ active, onClick, icon, label }) { return <button onClick={onClick} className={`flex items-center justify-center gap-2 rounded-xl px-3 py-3 text-sm font-medium transition ${active ? 'bg-indigo-500 text-white shadow-lg shadow-indigo-950/30' : 'text-zinc-500 hover:bg-white/5 hover:text-zinc-200'}`}>{icon}<span className="hidden sm:inline">{label}</span></button>; }
function EmptyState({ icon, title, description }) { return <div className="grid min-h-48 place-items-center rounded-2xl border border-dashed border-white/10 bg-zinc-900/30 p-8 text-center"><div><div className="mx-auto mb-3 text-zinc-600">{icon}</div><h3 className="font-medium text-zinc-300">{title}</h3><p className="mt-2 max-w-sm text-sm leading-relaxed text-zinc-600">{description}</p></div></div>; }
function ScreenLoader() { return <div className="grid min-h-48 place-items-center text-indigo-300"><Loader2 className="h-7 w-7 animate-spin" /></div>; }

export default App;
