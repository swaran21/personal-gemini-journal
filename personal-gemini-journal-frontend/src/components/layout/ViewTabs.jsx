import { BookHeart, MessageCircleHeart, Target } from 'lucide-react';

const tabs = [
  ['journal', BookHeart, 'Daily Journal'],
  ['rag', MessageCircleHeart, 'Chat with Past Self'],
  ['goals', Target, 'Accountability'],
];

export function ViewTabs({ view, setView }) {
  return <nav className="mb-8 flex w-full gap-2 overflow-x-auto rounded-2xl border border-[#E8E6E0] bg-white/60 p-1.5 sm:w-fit" aria-label="Journal views">{tabs.map(([id, Icon, label]) => <button key={id} onClick={() => setView(id)} className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition ${view === id ? 'bg-[#7A8D80] text-white shadow-sm' : 'text-[#7A756C] hover:bg-[#E8E6E0] hover:text-[#2D362E]'}`}><Icon className="h-4 w-4" />{label}</button>)}</nav>;
}
