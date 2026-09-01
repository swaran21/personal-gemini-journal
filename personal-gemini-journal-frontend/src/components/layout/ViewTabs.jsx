import { BookHeart, CalendarDays, MessageCircleHeart, Target, LayoutGrid, ShieldCheck } from 'lucide-react';

const tabs = [
  ['journal', BookHeart, 'Daily Journal'],
  ['rag', MessageCircleHeart, 'Chat with Past Self'],
  ['goals', Target, 'Accountability'],
  ['weekly', CalendarDays, 'Weekly Reflection'],
  ['calendar', LayoutGrid, 'Memory Calendar'],
];

export function ViewTabs({ view, setView, isAdmin }) {
  const visibleTabs = isAdmin ? [...tabs, ['admin', ShieldCheck, 'Admin Controls']] : tabs;
  return <nav className="mb-8 flex w-full gap-2 overflow-x-auto rounded-2xl border border-white/80 bg-white/75 p-1.5 shadow-sm backdrop-blur-xl sm:w-fit" aria-label="Journal views">{visibleTabs.map(([id, Icon, label], index) => <button key={id} onClick={() => setView(id)} className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all ${view === id ? ['bg-[#4285F4]','bg-[#34A853]','bg-[#EA4335]','bg-[#F9AB00]','bg-gradient-to-r from-[#4285F4] to-[#34A853]','bg-slate-700'][index] + ' text-white shadow-md' : 'text-slate-500 hover:bg-[#E8F0FE] hover:text-[#1967D2]'}`}><Icon className="h-4 w-4" />{label}</button>)}</nav>;
}
