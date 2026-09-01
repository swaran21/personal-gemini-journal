import { useState } from 'react';
import { CalendarDays, Loader2, Sparkles } from 'lucide-react';
import { api } from '../../api/client';

function InsightList({ title, items }) {
  return <section><h3 className="mb-3 text-[11px] font-bold uppercase tracking-[.18em] text-[#B87D64]">{title}</h3>{items.length ? <ul className="space-y-2">{items.map((item) => <li key={item} className="rounded-xl border border-[#E8E6E0] bg-white px-4 py-3 text-sm leading-relaxed">{item}</li>)}</ul> : <p className="text-sm text-[#9A968D]">Nothing confidently identified.</p>}</section>;
}

export function WeeklyReflectionView({ setError }) {
  const [reflection, setReflection] = useState(null);
  const [loading, setLoading] = useState(false);
  const generate = async () => {
    setLoading(true); setError('');
    try { setReflection(await api('/api/reflections/weekly', { method: 'POST', body: JSON.stringify({ timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone }) })); }
    catch (requestError) { setError(requestError.message); }
    finally { setLoading(false); }
  };
  return <div className="mx-auto max-w-5xl"><section className="rounded-3xl border border-[#E8E6E0] bg-white/60 p-6 sm:p-8"><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="flex items-center gap-2 text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]"><CalendarDays className="h-4 w-4" />Weekly reflection</p><h2 className="mt-2 font-serif text-3xl font-semibold">Notice the week, not just the day.</h2><p className="mt-2 max-w-2xl text-sm leading-relaxed text-[#9A968D]">Gemini reviews only this week&apos;s private entries to surface patterns, wins, unresolved themes, and one practical focus. These are reflection signals, not medical advice.</p></div><button type="button" onClick={generate} disabled={loading} className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-[#7A8D80] px-5 py-3 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60">{loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}{loading ? 'Reflecting...' : 'Reflect on this week'}</button></div></section>{reflection && <section className="mt-8 rounded-3xl border border-[#E8E6E0] bg-[#EBEBE4]/40 p-6 sm:p-8">{reflection.entryCount === 0 ? <div className="py-10 text-center"><p className="font-serif text-2xl">No entries this week yet.</p><p className="mt-2 text-sm text-[#9A968D]">Write a few memories first, then return for a grounded reflection.</p></div> : <><div className="mb-7 flex flex-wrap items-center justify-between gap-3"><p className="font-serif text-2xl font-semibold">Your week</p><span className="rounded-full border border-[#DCDCD2] bg-white px-3 py-1 text-xs text-[#7A756C]">Based on {reflection.entryCount} entries</span></div><div className="grid gap-7 md:grid-cols-3"><InsightList title="What stood out" items={reflection.highlights} /><InsightList title="Accomplishments" items={reflection.accomplishments} /><InsightList title="Still unresolved" items={reflection.unresolvedThemes} /></div><div className="mt-7 rounded-2xl bg-[#7A8D80] p-5 text-white"><p className="text-[11px] font-bold uppercase tracking-[.18em] text-white/70">Suggested focus</p><p className="mt-2 font-serif text-xl leading-relaxed">{reflection.suggestedFocus || 'Keep reflecting and choose one small next step.'}</p></div></>}</section>}</div>;
}
