import { GoalList } from '../actions/GoalList';
import { PageLoader } from '../common/PageLoader';

export function AccountabilityView({ items, setItems, loading, setError }) {
  const completed = items.filter((item) => item.status === 'COMPLETED').length;
  const percent = items.length ? Math.round((completed / items.length) * 100) : 0;
  return <div className="mx-auto max-w-5xl"><section className="mb-8 rounded-3xl border border-[#E8E6E0] bg-white/60 p-6 sm:p-8"><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]">Accountability Dashboard</p><h2 className="mt-2 font-serif text-3xl font-semibold">Small promises, visible progress.</h2><p className="mt-2 text-sm text-[#9A968D]">Gemini extracts commitments after each reflection.</p></div><div className="sm:text-right"><p className="text-3xl font-semibold text-[#7A8D80]">{percent}%</p><p className="text-sm text-[#9A968D]">{completed} of {items.length} complete</p></div></div><div className="mt-6 h-2 overflow-hidden rounded-full bg-[#E8E6E0]"><div className="h-full rounded-full bg-[#7A8D80] transition-all" style={{ width: `${percent}%` }} /></div></section>{loading ? <PageLoader /> : <div className="rounded-3xl border border-[#E8E6E0] bg-[#EBEBE4]/40 p-6 sm:p-8"><GoalList items={items} setItems={setItems} setError={setError} /></div>}</div>;
}
