import { useState } from 'react';
import { Loader2, Plus } from 'lucide-react';
import { GoalList } from '../actions/GoalList';
import { PageLoader } from '../common/PageLoader';
import { api } from '../../api/client';

export function AccountabilityView({ items, setItems, loading, setError, hasMore, loadMore }) {
  const [goal, setGoal] = useState('');
  const [saving, setSaving] = useState(false);
  const accepted = items.filter((item) => item.status !== 'PROPOSED');
  const completed = accepted.filter((item) => item.status === 'COMPLETED').length;
  const percent = accepted.length ? Math.round((completed / accepted.length) * 100) : 0;
  const addGoal = async (event) => {
    event.preventDefault();
    const value = goal.trim();
    if (!value || saving) return;
    setSaving(true); setError('');
    try {
      const created = await api('/api/action-items', { method: 'POST', body: JSON.stringify({ goal: value }) });
      setItems((current) => [created, ...current]); setGoal('');
    } catch (requestError) { setError(requestError.message); }
    finally { setSaving(false); }
  };
  return <div className="mx-auto max-w-5xl"><section className="mb-8 rounded-3xl border border-[#E8E6E0] bg-white/60 p-6 sm:p-8"><div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-[13px] font-bold uppercase tracking-[.2em] text-[#7A8D80]">Accountability Dashboard</p><h2 className="mt-2 font-serif text-3xl font-semibold">Small promises, visible progress.</h2><p className="mt-2 text-sm text-[#9A968D]">Add your own goal, or accept an AI-suggested commitment.</p></div><div className="sm:text-right"><p className="text-3xl font-semibold text-[#7A8D80]">{percent}%</p><p className="text-sm text-[#9A968D]">{completed} of {accepted.length} loaded goals complete</p></div></div><div className="mt-6 h-2 overflow-hidden rounded-full bg-[#E8E6E0]"><div className="h-full rounded-full bg-[#7A8D80] transition-all" style={{ width: `${percent}%` }} /></div><form onSubmit={addGoal} className="mt-6 flex gap-3"><input value={goal} onChange={(event) => setGoal(event.target.value)} maxLength={1000} className="min-w-0 flex-1 rounded-xl border border-[#DCDCD2] bg-white px-4 py-3 text-sm placeholder:text-[#9A968D]" placeholder="Add a goal you want to own..." aria-label="New accountability goal" /><button disabled={saving || !goal.trim()} className="inline-flex items-center gap-2 rounded-xl bg-[#7A8D80] px-4 py-3 text-sm font-semibold text-white hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50">{saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}<span className="hidden sm:inline">Add goal</span></button></form></section>{loading ? <PageLoader /> : <div className="rounded-3xl border border-[#E8E6E0] bg-[#EBEBE4]/40 p-6 sm:p-8"><GoalList items={items} setItems={setItems} setError={setError} />{hasMore && <div className="mt-6 text-center"><button type="button" onClick={loadMore} className="rounded-xl border border-[#DCDCD2] bg-white px-5 py-2.5 text-sm font-semibold text-[#7A8D80] hover:bg-[#F9F8F4]">Load older goals</button></div>}</div>}</div>;
}
