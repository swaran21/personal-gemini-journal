import { BookHeart, CalendarDays, MessageCircleHeart, Target, LayoutGrid } from 'lucide-react';

const tabs = [
  ['journal', BookHeart, 'Daily Journal'],
  ['rag', MessageCircleHeart, 'Chat with Past Self'],
  ['goals', Target, 'Accountability'],
  ['weekly', CalendarDays, 'Weekly Reflection'],
  ['calendar', LayoutGrid, 'Memory Calendar'],
];

export function ViewTabs({ view, setView }) {
  return <nav className="mb-8 flex w-full gap-2 overflow-x-auto rounded-2xl border border-white/70 bg-white/65 p-1.5 shadow-sm backdrop-blur-xl sm:w-fit" aria-label="Journal views">{tabs.map(([id, Icon, label], index) => <button key={id} onClick={() => setView(id)} className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all ${view === id ? ['bg-gradient-to-r from-violet-500 to-indigo-500','bg-gradient-to-r from-cyan-500 to-blue-500','bg-gradient-to-r from-rose-500 to-orange-400','bg-gradient-to-r from-emerald-500 to-teal-500','bg-gradient-to-r from-amber-400 to-rose-400'][index] + ' text-white shadow-md' : 'text-slate-500 hover:bg-violet-50 hover:text-violet-700'}`}><Icon className="h-4 w-4" />{label}</button>)}</nav>;
}
