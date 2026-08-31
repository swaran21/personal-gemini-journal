import { useState } from 'react';
import { Loader2, Send, Sparkles } from 'lucide-react';
import { api } from '../../api/client';
import { EntryCard } from './EntryCard';
import { InsightsAside } from '../actions/InsightsAside';

export function JournalView({ entries, setEntries, items, setItems, refreshItems, loading, setError }) {
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    const value = content.trim();
    if (!value || submitting) return;
    setSubmitting(true);
    setError('');
    try {
      const created = await api('/api/journal/entry', { method: 'POST', body: JSON.stringify({ content: value }) });
      setEntries((current) => [created, ...current]);
      setContent('');
      void refreshItems();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  return <div className="flex flex-col items-start gap-8 lg:flex-row lg:gap-12"><div className="flex w-full flex-1 flex-col gap-8"><section className="rounded-3xl border border-[#E8E6E0] bg-white/50 p-6 backdrop-blur-sm sm:p-8"><h2 className="mb-4 flex items-center gap-2 text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]"><Sparkles className="h-4 w-4" />What&apos;s on your mind?</h2><form onSubmit={submit} className="relative"><textarea value={content} onChange={(event) => setContent(event.target.value)} maxLength={10000} required className="min-h-[140px] w-full resize-y rounded-2xl border border-[#DCDCD2] bg-white px-6 py-5 pb-16 text-[15px] leading-relaxed text-[#2D362E] placeholder:text-[#9A968D] focus:ring-2 focus:ring-[#7A8D80]/20" placeholder="Write your thoughts here... Gemini will respond with empathy and extract actionable goals for you." /><button type="submit" disabled={submitting || !content.trim()} className="absolute bottom-4 right-4 grid h-11 w-11 place-items-center rounded-xl bg-[#7A8D80] text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50" aria-label="Save entry">{submitting ? <Loader2 className="h-5 w-5 animate-spin" /> : <Send className="h-5 w-5" />}</button></form><p className="mt-4 text-center text-[10px] uppercase tracking-widest text-[#9A968D]">Your authenticated private journal</p></section><section><h3 className="mb-8 text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]">Past Entries</h3>{entries.length ? <div className="flex flex-col gap-8">{entries.map((entry) => <EntryCard key={entry.id} entry={entry} />)}</div> : <p className="py-12 text-center text-[#9A968D]">{loading ? 'Loading memories...' : 'No entries yet. Start writing above to see them here!'}</p>}</section></div><InsightsAside items={items} setItems={setItems} setError={setError} /></div>;
}
