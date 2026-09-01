import { useState } from 'react';
import { Feather, Loader2, Send, Sparkles } from 'lucide-react';
import { api } from '../../api/client';
import { EntryCard } from './EntryCard';
import { LocationPicker } from './LocationPicker';
import { InsightsAside } from '../actions/InsightsAside';

const prompts = ['A small win today…', 'Something I want to remember…', 'What challenged me today…'];

export function JournalView({ entries, setEntries, items, setItems, refreshEntries, refreshItems, loading, setError, hasMore, loadMore }) {
  const [content, setContent] = useState('');
  const [location, setLocation] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const waitForAi = async (entryId) => {
    for (let attempt = 0; attempt < 15; attempt += 1) {
      await new Promise((resolve) => window.setTimeout(resolve, 2000));
      try {
        const latest = await refreshEntries();
        const entry = latest.find((value) => value.id === entryId);
        if (!entry || entry.processingStatus !== 'PENDING') {
          await refreshItems();
          return;
        }
      } catch (error) {
        if (attempt === 14) setError(error.message);
      }
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    const value = content.trim();
    if (!value || submitting) return;
    setSubmitting(true);
    setError('');
    try {
      const created = await api('/api/journal/entry', { method: 'POST', body: JSON.stringify({ content: value, location: location || undefined }) });
      setEntries((current) => [created, ...current]);
      setContent('');
      setLocation(null);
      void waitForAi(created.id);
    } catch (error) {
      setError(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  const retryAi = async (entryId) => {
    setError('');
    try {
      const pending = await api(`/api/journal/entries/${encodeURIComponent(entryId)}/retry`, { method: 'POST' });
      setEntries((current) => current.map((entry) => entry.id === entryId ? pending : entry));
      void waitForAi(entryId);
    } catch (error) {
      setError(error.message);
    }
  };

  const changed = (updated) => {
    setEntries((current) => current.map((entry) => entry.id === updated.id ? updated : entry));
    void waitForAi(updated.id);
  };

  return <div className="flex flex-col items-start gap-8 lg:flex-row lg:gap-10">
    <div className="flex w-full flex-1 flex-col gap-8">
      <section className="relative overflow-hidden rounded-[2rem] border border-white/80 bg-white/80 p-6 shadow-[0_25px_90px_-45px_rgba(66,133,244,.5)] backdrop-blur-xl sm:p-8">
        <div className="absolute -right-16 -top-20 h-52 w-52 rounded-full bg-[#D2E3FC]/70 blur-2xl" />
        <div className="absolute -bottom-20 -left-10 h-44 w-44 rounded-full bg-[#FEF7E0] blur-2xl" />
        <div className="relative">
          <div className="mb-5 flex items-center justify-between">
            <div><p className="eyebrow text-[#4285F4]">Daily journal</p><h2 className="mt-2 flex items-center gap-2 font-serif text-3xl font-semibold text-slate-800"><Feather className="h-6 w-6 text-[#EA4335]" />What&apos;s alive in you?</h2></div>
            <div className="google-logo-gradient hidden h-12 w-12 place-items-center rounded-2xl text-white shadow-lg shadow-blue-100 sm:grid"><Sparkles className="h-5 w-5" /></div>
          </div>
          <div className="mb-4 flex gap-2 overflow-x-auto pb-1">
            {prompts.map((prompt, index) => <button key={prompt} type="button" onClick={() => setContent((current) => current || `${prompt} `)} className={`shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition ${['border-[#AECBFA] bg-[#E8F0FE] text-[#1967D2] hover:bg-[#D2E3FC]', 'border-[#FAD2CF] bg-[#FCE8E6] text-[#C5221F] hover:bg-[#FAD2CF]', 'border-[#CEEAD6] bg-[#E6F4EA] text-[#137333] hover:bg-[#CEEAD6]'][index]}`}>{prompt}</button>)}
          </div>
          <form onSubmit={submit}>
            <div className="relative">
              <textarea value={content} onChange={(event) => setContent(event.target.value)} maxLength={10000} required className="min-h-[170px] w-full resize-y rounded-3xl border border-[#D2E3FC] bg-white/95 px-6 py-5 pb-16 text-[15px] leading-relaxed text-slate-700 shadow-inner placeholder:text-slate-400 focus:border-[#4285F4] focus:ring-4 focus:ring-[#E8F0FE]" placeholder="Write freely. Your words are saved first, then Gemini reflects in the background…" />
              <button type="submit" disabled={submitting || !content.trim()} className="google-action-gradient absolute bottom-4 right-4 inline-flex h-12 items-center gap-2 rounded-2xl px-5 font-semibold text-white shadow-lg shadow-blue-200 transition hover:-translate-y-0.5 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-50" aria-label="Save entry">{submitting ? <Loader2 className="h-5 w-5 animate-spin" /> : <Send className="h-4 w-4" />}<span className="hidden sm:inline">Save memory</span></button>
            </div>
            <LocationPicker location={location} onChange={setLocation} setError={setError} />
          </form>
          <p className="mt-4 text-center text-[10px] font-semibold uppercase tracking-[.18em] text-slate-400">Private by design · Location only when you approve it</p>
        </div>
      </section>

      <section>
        <div className="mb-7 flex items-end justify-between"><div><p className="eyebrow text-[#34A853]">Your story</p><h3 className="mt-1 font-serif text-2xl font-semibold text-slate-800">Past entries</h3></div><span className="rounded-full bg-[#E8F0FE] px-3 py-1 text-xs font-bold text-[#1967D2]">{entries.length} loaded</span></div>
        {entries.length ? <><div className="flex flex-col gap-7">{entries.map((entry, index) => <EntryCard key={entry.id} entry={entry} index={index} onRetry={retryAi} onChanged={changed} onDeleted={(id) => setEntries((current) => current.filter((entry) => entry.id !== id))} setError={setError} />)}</div>{hasMore && <div className="mt-8 text-center"><button type="button" onClick={loadMore} className="rounded-2xl border border-[#AECBFA] bg-white px-6 py-3 text-sm font-semibold text-[#1967D2] shadow-sm transition hover:-translate-y-0.5 hover:bg-[#F8FBFF] hover:shadow-md">Load older memories</button></div>}</> : <div className="glass-card py-16 text-center text-slate-400">{loading ? 'Gathering your memories…' : 'No memories yet. Your story can start above.'}</div>}
      </section>
    </div>
    <InsightsAside items={items} setItems={setItems} setError={setError} />
  </div>;
}
