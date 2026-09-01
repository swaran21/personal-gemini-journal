import { useEffect, useMemo, useState } from 'react';
import { CalendarDays, ChevronLeft, ChevronRight, MapPin, Sparkles } from 'lucide-react';
import { api } from '../../api/client';

const weekdayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function localDateKey(value) {
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function CalendarView({ setError }) {
  const [month, setMonth] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const [entries, setEntries] = useState([]);
  const [selected, setSelected] = useState(() => localDateKey(new Date()));
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let active = true; setLoading(true); setError('');
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    api(`/api/journal/calendar?year=${month.getFullYear()}&month=${month.getMonth() + 1}&timeZone=${encodeURIComponent(zone)}`)
      .then((data) => { if (active) setEntries(data); })
      .catch((error) => { if (active) setError(error.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [month, setError]);

  const grouped = useMemo(() => entries.reduce((result, entry) => {
    const key = localDateKey(entry.createdAt); (result[key] ||= []).push(entry); return result;
  }, {}), [entries]);
  const days = useMemo(() => {
    const first = new Date(month.getFullYear(), month.getMonth(), 1); const count = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
    return [...Array(first.getDay()).fill(null), ...Array.from({ length: count }, (_, index) => index + 1)];
  }, [month]);
  const selectedEntries = grouped[selected] || [];
  const move = (amount) => { const next = new Date(month.getFullYear(), month.getMonth() + amount, 1); setMonth(next); setSelected(`${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}-01`); };

  return <div className="grid gap-7 lg:grid-cols-[1.35fr_.85fr]">
    <section className="overflow-hidden rounded-[2rem] border border-white/70 bg-white/75 p-5 shadow-[0_24px_80px_-40px_rgba(70,60,120,.45)] backdrop-blur-xl sm:p-8">
      <div className="mb-7 flex items-center justify-between"><div><p className="eyebrow text-violet-600">Memory calendar</p><h2 className="mt-2 font-serif text-3xl font-semibold text-slate-800">{month.toLocaleString(undefined, { month: 'long', year: 'numeric' })}</h2></div><div className="flex gap-2"><button onClick={() => move(-1)} className="icon-button" aria-label="Previous month"><ChevronLeft /></button><button onClick={() => move(1)} className="icon-button" aria-label="Next month"><ChevronRight /></button></div></div>
      <div className="grid grid-cols-7 gap-2">{weekdayNames.map(day => <div key={day} className="pb-2 text-center text-[10px] font-bold uppercase tracking-[.15em] text-slate-400">{day}</div>)}{days.map((day, index) => {
        if (!day) return <div key={`blank-${index}`} />;
        const key = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`; const count = grouped[key]?.length || 0; const active = selected === key; const today = key === localDateKey(new Date());
        return <button key={key} onClick={() => setSelected(key)} className={`relative aspect-square rounded-2xl border p-2 text-left transition-all hover:-translate-y-0.5 hover:shadow-md ${active ? 'border-violet-400 bg-gradient-to-br from-violet-500 to-indigo-500 text-white shadow-lg shadow-violet-200' : 'border-slate-100 bg-white/80 text-slate-700 hover:border-violet-200'} ${today && !active ? 'ring-2 ring-amber-300' : ''}`}><span className="text-sm font-semibold">{day}</span>{count > 0 && <div className="absolute bottom-2 left-2 right-2"><div className={`h-1.5 rounded-full ${active ? 'bg-white/80' : 'bg-gradient-to-r from-fuchsia-400 to-amber-400'}`} /><span className={`mt-1 block text-[9px] ${active ? 'text-white/80' : 'text-slate-400'}`}>{count} {count === 1 ? 'memory' : 'memories'}</span></div>}</button>;
      })}</div>{loading && <p className="mt-5 text-center text-sm text-violet-500">Gathering this month&apos;s memories…</p>}
    </section>
    <aside className="rounded-[2rem] border border-white/70 bg-gradient-to-br from-indigo-50/90 via-white/90 to-amber-50/90 p-6 shadow-[0_24px_80px_-45px_rgba(70,60,120,.5)] sm:p-8"><div className="flex items-center gap-3"><div className="grid h-11 w-11 place-items-center rounded-2xl bg-gradient-to-br from-amber-400 to-rose-400 text-white shadow-lg shadow-rose-200"><CalendarDays className="h-5 w-5" /></div><div><p className="eyebrow text-rose-500">Selected day</p><h3 className="font-serif text-xl font-semibold text-slate-800">{new Date(`${selected}T12:00:00`).toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}</h3></div></div><div className="mt-6 space-y-4">{selectedEntries.length ? selectedEntries.map(entry => <article key={entry.id} className="rounded-2xl border border-white bg-white/85 p-4 shadow-sm"><p className="line-clamp-4 whitespace-pre-wrap text-sm leading-relaxed text-slate-700">{entry.content}</p>{entry.location && <p className="mt-3 flex items-center gap-1.5 text-xs font-medium text-rose-500"><MapPin className="h-3.5 w-3.5" />{entry.location.label || 'Pinned location'}</p>}{entry.aiResponse && <div className="mt-3 rounded-xl bg-violet-50 p-3 text-xs leading-relaxed text-violet-800"><Sparkles className="mr-1 inline h-3.5 w-3.5" />{entry.aiResponse}</div>}</article>) : <div className="rounded-2xl border border-dashed border-violet-200 bg-white/55 p-8 text-center"><CalendarDays className="mx-auto h-8 w-8 text-violet-300" /><p className="mt-3 font-medium text-slate-600">No memories on this day</p><p className="mt-1 text-xs text-slate-400">Choose a colorful day or write a new reflection.</p></div>}</div></aside>
  </div>;
}
