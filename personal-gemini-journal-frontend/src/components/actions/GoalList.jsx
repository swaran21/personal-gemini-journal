import { useState } from 'react';
import { Check, CheckCircle2, Circle, Loader2, Trash2, X } from 'lucide-react';
import { api } from '../../api/client';

export function GoalList({ items, setItems, setError, compact = false }) {
  const [updating, setUpdating] = useState(new Set());

  const toggle = async (item) => {
    if (updating.has(item.id)) return;
    const before = item.status;
    const status = before === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    setUpdating((current) => new Set(current).add(item.id));
    setItems((current) => current.map((value) => value.id === item.id ? { ...value, status } : value));
    try {
      await api(`/api/action-items/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status }) });
    } catch (requestError) {
      setItems((current) => current.map((value) => value.id === item.id ? { ...value, status: before } : value));
      setError(requestError.message);
    } finally {
      setUpdating((current) => { const next = new Set(current); next.delete(item.id); return next; });
    }
  };

  const accept = async (item) => {
    if (updating.has(item.id)) return;
    setUpdating((current) => new Set(current).add(item.id));
    setItems((current) => current.map((value) => value.id === item.id ? { ...value, status: 'PENDING' } : value));
    try {
      await api(`/api/action-items/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status: 'PENDING' }) });
    } catch (requestError) {
      setItems((current) => current.map((value) => value.id === item.id ? { ...value, status: 'PROPOSED' } : value));
      setError(requestError.message);
    } finally {
      setUpdating((current) => { const next = new Set(current); next.delete(item.id); return next; });
    }
  };

  const remove = async (event, id) => {
    event.stopPropagation();
    const removed = items.find((item) => item.id === id);
    setItems((current) => current.filter((item) => item.id !== id));
    try {
      await api(`/api/action-items/${id}`, { method: 'DELETE' });
    } catch (requestError) {
      if (removed) setItems((current) => [...current, removed]);
      setError(requestError.message);
    }
  };

  if (!items.length) return <p className="rounded-3xl border border-dashed border-[#DCDCD2] bg-white py-8 text-center text-[13px] text-[#9A968D]">No action suggestions yet.<br />AI will suggest commitments without adding them automatically.</p>;

  return <div className={`flex flex-col ${compact ? 'gap-4' : 'gap-5'}`}>{items.map((item) => <article key={item.id} className={`group relative flex w-full items-start gap-3 overflow-hidden rounded-3xl border p-5 text-left shadow-sm transition-all ${item.status === 'COMPLETED' ? 'border-[#E8E6E0] bg-[#F9F8F4] opacity-70' : 'border-[#DCDCD2] bg-white'}`}><button type="button" onClick={() => item.status !== 'PROPOSED' && toggle(item)} disabled={item.status === 'PROPOSED' || updating.has(item.id)} className="mt-0.5 text-[#9A968D]" aria-label={item.status === 'PROPOSED' ? 'Suggested goal' : `Mark ${item.goal} ${item.status === 'COMPLETED' ? 'pending' : 'completed'}`}>{updating.has(item.id) ? <Loader2 className="h-5 w-5 animate-spin" /> : item.status === 'COMPLETED' ? <CheckCircle2 className="h-5 w-5 text-[#7A8D80]" /> : <Circle className="h-5 w-5" />}</button>{item.status === 'PENDING' && <span className="absolute left-0 top-0 h-full w-1 bg-[#B87D64]" />}<div className="flex-1"><span className={`block text-[11px] font-bold uppercase tracking-wider ${item.status === 'COMPLETED' ? 'text-[#9A968D]' : 'text-[#B87D64]'}`}>{item.status === 'PROPOSED' ? 'Suggested action' : item.status === 'COMPLETED' ? 'Completed' : 'Action Item'}</span><span className={`mt-1 block text-[14px] font-medium leading-snug ${item.status === 'COMPLETED' ? 'text-[#9A968D] line-through' : 'text-[#2D362E]'}`}>{item.goal}</span>{item.status === 'PROPOSED' && <div className="mt-4 flex gap-2"><button type="button" onClick={() => accept(item)} className="inline-flex items-center gap-1.5 rounded-lg bg-[#7A8D80] px-3 py-2 text-xs font-semibold text-white hover:opacity-90"><Check className="h-3.5 w-3.5" />Add goal</button><button type="button" onClick={(event) => remove(event, item.id)} className="inline-flex items-center gap-1.5 rounded-lg border border-[#DCDCD2] px-3 py-2 text-xs font-semibold text-[#7A756C] hover:bg-[#F9F8F4]"><X className="h-3.5 w-3.5" />Dismiss</button></div>}</div>{item.status !== 'PROPOSED' && <button type="button" onClick={(event) => remove(event, item.id)} className="rounded-full p-2 text-[#9A968D] opacity-0 transition-opacity hover:bg-[#F9F8F4] hover:text-red-500 group-hover:opacity-100" aria-label={`Delete ${item.goal}`}><Trash2 className="h-4 w-4" /></button>}</article>)}</div>;
}
