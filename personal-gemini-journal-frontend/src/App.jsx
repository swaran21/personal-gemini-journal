import { useCallback, useEffect, useState } from 'react';
import { onAuthStateChanged, signInWithPopup, signOut } from 'firebase/auth';
import {
  BookHeart, Bot, CheckCircle2, ChevronDown, ChevronUp, Circle, Loader2,
  LogIn, LogOut, MessageCircleHeart, Send, Sparkles, Target, Trash2, X,
} from 'lucide-react';
import { auth, googleProvider } from './firebase';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function api(user, path, options = {}) {
  const token = await user.getIdToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.error || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
}

function dayLabel(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Today' : date.toLocaleString(undefined, { weekday: 'long', month: 'short', day: 'numeric' });
}

function timeLabel(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Just now' : date.toLocaleString(undefined, { hour: 'numeric', minute: '2-digit' });
}

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
        api(currentUser, '/api/journal/entries'),
        api(currentUser, '/api/action-items'),
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

  if (!authReady) return <PageLoader />;
  if (!user) return <LoginScreen />;

  return (
    <div className="min-h-screen bg-[#F9F8F4] text-[#2D362E] selection:bg-[#7A8D80]/20">
      <Header user={user} />
      <main className="mx-auto max-w-7xl px-6 py-8 lg:px-10 lg:py-10">
        <ViewTabs view={view} setView={setView} />
        {error && <ErrorBanner message={error} onClose={() => setError('')} />}
        {view === 'journal' && <JournalView user={user} entries={entries} setEntries={setEntries} items={actionItems} setItems={setActionItems} loading={loading} setError={setError} />}
        {view === 'rag' && <RagView user={user} setError={setError} />}
        {view === 'goals' && <AccountabilityView user={user} items={actionItems} setItems={setActionItems} loading={loading} setError={setError} />}
      </main>
    </div>
  );
}

function Header({ user }) {
  return <header className="sticky top-0 z-20 border-b border-[#E8E6E0] bg-[#F9F8F4]/80 backdrop-blur-md">
    <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-6 lg:px-10">
      <div className="flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-[#7A8D80] text-white"><BookHeart className="h-6 w-6" /></div><h1 className="font-serif text-2xl font-semibold tracking-tight">Gemini Journal</h1></div>
      <div className="flex items-center gap-3 sm:gap-4">
        <span className="hidden rounded-full bg-[#E8EBE8] px-3 py-1 text-sm font-medium text-[#7A8D80] sm:inline">Secure Session Active</span>
        <div className="hidden items-center gap-2 md:flex">{user.photoURL ? <img className="h-8 w-8 rounded-full" src={user.photoURL} alt="Your profile" referrerPolicy="no-referrer" /> : <div className="grid h-8 w-8 place-items-center rounded-full bg-[#E8EBE8] text-xs font-bold text-[#7A8D80]">{user.email?.[0]?.toUpperCase()}</div>}<span className="max-w-44 truncate text-sm text-[#6F716A]">{user.email}</span></div>
        <button onClick={() => signOut(auth)} className="rounded-full p-2 text-[#9A968D] transition-colors hover:bg-[#E8E6E0] hover:text-[#2D362E]" aria-label="Sign out"><LogOut className="h-5 w-5" /></button>
      </div>
    </div>
  </header>;
}

function ViewTabs({ view, setView }) {
  const tabs = [['journal', <BookHeart className="h-4 w-4" />, 'Daily Journal'], ['rag', <MessageCircleHeart className="h-4 w-4" />, 'Chat with Past Self'], ['goals', <Target className="h-4 w-4" />, 'Accountability']];
  return <nav className="mb-8 flex w-full gap-2 overflow-x-auto rounded-2xl border border-[#E8E6E0] bg-white/60 p-1.5 sm:w-fit" aria-label="Journal views">{tabs.map(([id, icon, label]) => <button key={id} onClick={() => setView(id)} className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition ${view === id ? 'bg-[#7A8D80] text-white shadow-sm' : 'text-[#7A756C] hover:bg-[#E8E6E0] hover:text-[#2D362E]'}`}>{icon}{label}</button>)}</nav>;
}

function LoginScreen() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const login = async () => { setLoading(true); setError(''); try { await signInWithPopup(auth, googleProvider); } catch (signInError) { setError(signInError.message.replace('Firebase: ', '')); } finally { setLoading(false); } };
  return <div className="flex min-h-screen items-center justify-center bg-[#F9F8F4] p-6"><section className="w-full max-w-md rounded-3xl border border-[#DCDCD2] bg-white p-8 text-center shadow-sm"><div className="mx-auto mb-6 grid h-16 w-16 place-items-center rounded-2xl bg-[#7A8D80] text-white"><BookHeart className="h-8 w-8" /></div><h1 className="font-serif text-2xl font-semibold tracking-tight text-[#2D362E]">Gemini Journal</h1><p className="mt-3 leading-relaxed text-[#9A968D]">A private, secure space for reflection. Powered by AI to help you find clarity and track actionable goals.</p>{error && <p className="mt-5 rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}<button onClick={login} disabled={loading} className="mt-8 flex w-full items-center justify-center gap-3 rounded-xl bg-[#7A8D80] px-6 py-3.5 font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50">{loading ? <Loader2 className="h-5 w-5 animate-spin" /> : <LogIn className="h-5 w-5" />}{loading ? 'Signing in...' : 'Sign in with Google'}</button></section></div>;
}

function JournalView({ user, entries, setEntries, items, setItems, loading, setError }) {
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submit = async (event) => { event.preventDefault(); const value = content.trim(); if (!value || submitting) return; setSubmitting(true); setError(''); try { const created = await api(user, '/api/journal/entry', { method: 'POST', body: JSON.stringify({ content: value }) }); setEntries((current) => [created, ...current]); setContent(''); } catch (requestError) { setError(requestError.message); } finally { setSubmitting(false); } };
  return <div className="flex flex-col items-start gap-8 lg:flex-row lg:gap-12">
    <div className="flex w-full flex-1 flex-col gap-8">
      <section className="rounded-3xl border border-[#E8E6E0] bg-white/50 p-6 backdrop-blur-sm sm:p-8"><h2 className="mb-4 flex items-center gap-2 text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]"><Sparkles className="h-4 w-4" />What&apos;s on your mind?</h2><form onSubmit={submit} className="relative"><textarea value={content} onChange={(event) => setContent(event.target.value)} maxLength={10000} required className="min-h-[140px] w-full resize-y rounded-2xl border border-[#DCDCD2] bg-white px-6 py-5 pb-16 text-[15px] leading-relaxed text-[#2D362E] placeholder:text-[#9A968D] focus:ring-2 focus:ring-[#7A8D80]/20" placeholder="Write your thoughts here... Gemini will respond with empathy and extract actionable goals for you." /><button type="submit" disabled={submitting || !content.trim()} className="absolute bottom-4 right-4 grid h-11 w-11 place-items-center rounded-xl bg-[#7A8D80] text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50" aria-label="Save entry">{submitting ? <Loader2 className="h-5 w-5 animate-spin" /> : <Send className="h-5 w-5" />}</button></form><p className="mt-4 text-center text-[10px] uppercase tracking-widest text-[#9A968D]">Your authenticated private journal</p></section>
      <section><h3 className="mb-8 text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]">Past Entries</h3>{entries.length ? <div className="flex flex-col gap-8">{entries.map((entry) => <EntryCard key={entry.id} entry={entry} />)}</div> : <p className="py-12 text-center text-[#9A968D]">{loading ? 'Loading memories...' : 'No entries yet. Start writing above to see them here!'}</p>}</section>
    </div>
    <InsightsAside user={user} items={items} setItems={setItems} setError={setError} />
  </div>;
}

function EntryCard({ entry }) {
  return <article className="flex flex-col gap-6"><div className="flex justify-center"><span className="rounded-full border border-[#E8E6E0] px-4 py-1 text-[11px] font-bold uppercase tracking-[.2em] text-[#9A968D]">{dayLabel(entry.createdAt)}</span></div><div className="flex flex-col items-end gap-2"><div className="max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-tr-none bg-[#7A8D80] px-5 py-4 text-[15px] leading-relaxed text-white shadow-sm sm:max-w-[80%]">{entry.content}</div><span className="text-[10px] font-medium uppercase text-[#9A968D]">Sent {timeLabel(entry.createdAt)}</span></div>{entry.aiResponse && <div className="flex flex-col items-start gap-2"><div className="max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-tl-none border border-[#E8E6E0] bg-white px-5 py-4 text-[15px] leading-relaxed text-[#3A3A35] shadow-sm sm:max-w-[80%]"><p>{entry.aiResponse}</p></div><span className="text-[10px] font-medium uppercase text-[#9A968D]">Gemini Analysis - {timeLabel(entry.createdAt)}</span></div>}{entry.extractedGoal && <div className="flex justify-end"><span className="max-w-[85%] rounded-full bg-[#F9E8E1] px-3 py-1.5 text-xs font-medium text-[#A45D43]">Goal: {entry.extractedGoal}</span></div>}</article>;
}

function InsightsAside({ user, items, setItems, setError }) {
  const pending = items.filter((item) => item.status === 'PENDING').length;
  return <aside className="w-full rounded-3xl border border-[#E8E6E0] bg-[#EBEBE4]/40 p-8 lg:sticky lg:top-24 lg:w-[400px]"><div className="mb-6 flex items-center justify-between"><h2 className="flex items-center gap-2 text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]"><span className="h-2 w-2 rounded-full bg-[#B87D64]" />Extracted Insights</h2><span className="rounded-full border border-[#E8E6E0] bg-[#F9F8F4] px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-[#9A968D]">{pending} Pending</span></div><GoalList user={user} items={items} setItems={setItems} setError={setError} compact /></aside>;
}

function RagView({ user, setError }) {
  const [query, setQuery] = useState(''); const [messages, setMessages] = useState([]); const [asking, setAsking] = useState(false);
  const ask = async (event) => { event.preventDefault(); const value = query.trim(); if (!value || asking) return; setAsking(true); setError(''); setMessages((current) => [...current, { role: 'user', text: value }]); setQuery(''); try { const result = await api(user, '/api/chat/rag', { method: 'POST', body: JSON.stringify({ query: value }) }); setMessages((current) => [...current, { role: 'assistant', ...result }]); } catch (requestError) { setMessages((current) => current.slice(0, -1)); setError(requestError.message); } finally { setAsking(false); } };
  return <section className="mx-auto flex min-h-[600px] max-w-4xl flex-col overflow-hidden rounded-3xl border border-[#E8E6E0] bg-white/60"><div className="border-b border-[#E8E6E0] p-6 sm:p-8"><div className="flex items-center gap-3"><div className="grid h-11 w-11 place-items-center rounded-xl bg-[#E8EBE8] text-[#7A8D80]"><MessageCircleHeart className="h-5 w-5" /></div><div><h2 className="font-serif text-2xl font-semibold">Chat with Past Self</h2><p className="mt-1 text-sm text-[#9A968D]">Grounded only in your own private reflections.</p></div></div></div><div className="flex-1 space-y-5 p-5 sm:p-8">{!messages.length && <EmptyState icon={<MessageCircleHeart className="h-7 w-7" />} title="Ask your past self" description="Try: When did I feel most productive last week?" />}{messages.map((message, index) => message.role === 'user' ? <div key={index} className="ml-auto max-w-[85%] rounded-2xl rounded-tr-none bg-[#7A8D80] px-5 py-4 text-[15px] leading-relaxed text-white">{message.text}</div> : <RagReply key={index} message={message} />)}{asking && <div className="flex items-center gap-2 text-sm text-[#9A968D]"><Loader2 className="h-4 w-4 animate-spin" />Searching your memories...</div>}</div><form onSubmit={ask} className="border-t border-[#E8E6E0] p-4 sm:p-5"><div className="flex gap-3"><input value={query} onChange={(event) => setQuery(event.target.value)} maxLength={4000} className="min-w-0 flex-1 rounded-xl border border-[#DCDCD2] bg-white px-4 py-3 text-sm placeholder:text-[#9A968D]" placeholder="Ask about a memory, habit, or feeling..." /><button disabled={asking || !query.trim()} className="grid h-11 w-11 place-items-center rounded-xl bg-[#7A8D80] text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50" aria-label="Ask past self"><Send className="h-4 w-4" /></button></div></form></section>;
}

function RagReply({ message }) { const [open, setOpen] = useState(false); return <div className="max-w-[92%] rounded-2xl rounded-tl-none border border-[#E8E6E0] bg-white p-5 text-[15px] leading-relaxed text-[#3A3A35]"><div className="mb-2 flex items-center gap-2 text-[11px] font-bold uppercase tracking-[.16em] text-[#7A8D80]"><Bot className="h-3.5 w-3.5" />Past Self</div><p className="whitespace-pre-wrap">{message.reply}</p>{message.referencedEntries?.length > 0 && <div className="mt-4 border-t border-[#E8E6E0] pt-3"><button onClick={() => setOpen(!open)} className="flex w-full items-center justify-between text-xs font-medium text-[#7A8D80]">Referenced memories <span className="flex items-center gap-1">{message.referencedEntries.length}{open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}</span></button>{open && <ul className="mt-3 space-y-2">{message.referencedEntries.map((reference) => <li key={reference} className="rounded-lg bg-[#F9F8F4] px-3 py-2 text-xs text-[#7A756C]">Memory {reference}</li>)}</ul>}</div>}</div>; }

function AccountabilityView({ user, items, setItems, loading, setError }) {
  const completed = items.filter((item) => item.status === 'COMPLETED').length;
  const percent = items.length ? Math.round((completed / items.length) * 100) : 0;
  return <div className="mx-auto max-w-5xl"><section className="mb-8 rounded-3xl border border-[#E8E6E0] bg-white/60 p-6 sm:p-8"><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]">Accountability Dashboard</p><h2 className="mt-2 font-serif text-3xl font-semibold">Small promises, visible progress.</h2><p className="mt-2 text-sm text-[#9A968D]">Gemini extracts commitments after each reflection.</p></div><div className="sm:text-right"><p className="text-3xl font-semibold text-[#7A8D80]">{percent}%</p><p className="text-sm text-[#9A968D]">{completed} of {items.length} complete</p></div></div><div className="mt-6 h-2 overflow-hidden rounded-full bg-[#E8E6E0]"><div className="h-full rounded-full bg-[#7A8D80] transition-all" style={{ width: `${percent}%` }} /></div></section>{loading ? <PageLoader /> : <div className="rounded-3xl border border-[#E8E6E0] bg-[#EBEBE4]/40 p-6 sm:p-8"><GoalList user={user} items={items} setItems={setItems} setError={setError} /></div>}</div>;
}

function GoalList({ user, items, setItems, setError, compact = false }) {
  const [updating, setUpdating] = useState(new Set());
  const toggle = async (item) => { if (updating.has(item.id)) return; const before = item.status; const status = before === 'COMPLETED' ? 'PENDING' : 'COMPLETED'; setUpdating((current) => new Set(current).add(item.id)); setItems((current) => current.map((value) => value.id === item.id ? { ...value, status } : value)); try { await api(user, `/api/action-items/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status }) }); } catch (requestError) { setItems((current) => current.map((value) => value.id === item.id ? { ...value, status: before } : value)); setError(requestError.message); } finally { setUpdating((current) => { const next = new Set(current); next.delete(item.id); return next; }); } };
  const remove = async (event, id) => { event.stopPropagation(); const removed = items.find((item) => item.id === id); setItems((current) => current.filter((item) => item.id !== id)); try { await api(user, `/api/action-items/${id}`, { method: 'DELETE' }); } catch (requestError) { if (removed) setItems((current) => [...current, removed]); setError(requestError.message); } };
  if (!items.length) return <p className="rounded-3xl border border-dashed border-[#DCDCD2] bg-white py-8 text-center text-[13px] text-[#9A968D]">No action items yet.<br />Gemini will automatically extract goals from your entries.</p>;
  return <div className={`flex flex-col ${compact ? 'gap-4' : 'gap-5'}`}>{items.map((item) => <button key={item.id} onClick={() => toggle(item)} className={`group relative flex w-full items-start gap-3 overflow-hidden rounded-3xl border p-5 text-left shadow-sm transition-all ${item.status === 'COMPLETED' ? 'border-[#E8E6E0] bg-[#F9F8F4] opacity-70' : 'border-[#DCDCD2] bg-white'}`}><span className="mt-0.5 text-[#9A968D]">{updating.has(item.id) ? <Loader2 className="h-5 w-5 animate-spin" /> : item.status === 'COMPLETED' ? <CheckCircle2 className="h-5 w-5 text-[#7A8D80]" /> : <Circle className="h-5 w-5" />}</span>{item.status === 'PENDING' && <span className="absolute left-0 top-0 h-full w-1 bg-[#B87D64]" />}<span className="flex-1"><span className={`block text-[11px] font-bold uppercase tracking-wider ${item.status === 'COMPLETED' ? 'text-[#9A968D]' : 'text-[#B87D64]'}`}>{item.status === 'COMPLETED' ? 'Completed' : 'Action Item'}</span><span className={`mt-1 block text-[14px] font-medium leading-snug ${item.status === 'COMPLETED' ? 'text-[#9A968D] line-through' : 'text-[#2D362E]'}`}>{item.goal}</span></span><span onClick={(event) => remove(event, item.id)} className="rounded-full p-2 text-[#9A968D] opacity-0 transition-opacity hover:bg-[#F9F8F4] hover:text-red-500 group-hover:opacity-100" role="button" aria-label={`Delete ${item.goal}`}><Trash2 className="h-4 w-4" /></span></button>)}</div>;
}

function ErrorBanner({ message, onClose }) { return <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><X className="mt-0.5 h-4 w-4 shrink-0" /><p className="flex-1">{message}</p><button onClick={onClose} aria-label="Dismiss error">x</button></div>; }
function EmptyState({ icon, title, description }) { return <div className="grid min-h-48 place-items-center rounded-2xl border border-dashed border-[#DCDCD2] bg-white/50 p-8 text-center"><div><div className="mx-auto mb-3 text-[#9A968D]">{icon}</div><h3 className="font-medium text-[#2D362E]">{title}</h3><p className="mt-2 max-w-sm text-sm leading-relaxed text-[#9A968D]">{description}</p></div></div>; }
function PageLoader() { return <div className="grid min-h-48 place-items-center text-[#7A8D80]"><Loader2 className="h-7 w-7 animate-spin" /></div>; }
