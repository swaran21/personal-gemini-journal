import { GoalList } from './GoalList';

export function InsightsAside({ items, setItems, setError }) {
  const pending = items.filter((item) => item.status === 'PENDING').length;
  return <aside className="w-full rounded-3xl border border-[#E8E6E0] bg-[#EBEBE4]/40 p-8 lg:sticky lg:top-24 lg:w-[400px]"><div className="mb-6 flex items-center justify-between"><h2 className="flex items-center gap-2 text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]"><span className="h-2 w-2 rounded-full bg-[#B87D64]" />Extracted Insights</h2><span className="rounded-full border border-[#E8E6E0] bg-[#F9F8F4] px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-[#9A968D]">{pending} Pending</span></div><GoalList items={items} setItems={setItems} setError={setError} compact /></aside>;
}
