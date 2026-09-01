import { useEffect, useState } from 'react';
import { CheckCircle2, ShieldCheck, LockKeyhole, RefreshCw } from 'lucide-react';
import { api } from '../../api/client';
import { PageLoader } from '../common/PageLoader';

/**
 * Shows only safe operational controls. It deliberately has no cross-user data,
 * metrics, journal content, or administrative mutation capability.
 */
export function AdminDashboardView({ setError }) {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try { setDashboard(await api('/api/admin/dashboard')); }
    catch (error) { setError(error.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);
  if (loading) return <PageLoader />;
  if (!dashboard) return <section className="rounded-3xl border border-rose-100 bg-rose-50 p-8 text-rose-700">Admin controls could not be loaded.</section>;

  return <section className="overflow-hidden rounded-[2rem] border border-white/80 bg-white/85 shadow-[0_24px_80px_-45px_rgba(30,41,59,.55)] backdrop-blur">
    <div className="bg-gradient-to-r from-slate-800 via-slate-700 to-[#4285F4] px-6 py-8 text-white sm:px-9"><div className="flex items-start justify-between gap-4"><div><p className="eyebrow text-blue-100">Verified elevated role</p><h2 className="mt-2 flex items-center gap-3 font-serif text-3xl font-semibold"><ShieldCheck className="h-8 w-8" />Admin controls</h2><p className="mt-3 max-w-2xl text-sm leading-6 text-slate-200">Security controls are visible here; personal journal content is not.</p></div><button type="button" onClick={load} className="icon-button !border-white/20 !bg-white/10 !text-white hover:!bg-white/20" aria-label="Refresh admin controls"><RefreshCw className="h-4 w-4" /></button></div></div>
    <div className="p-6 sm:p-9"><div className="mb-7 flex gap-3 rounded-2xl border border-[#D2E3FC] bg-[#E8F0FE] p-4 text-sm leading-6 text-[#174EA6]"><LockKeyhole className="mt-0.5 h-5 w-5 shrink-0" /><p>{dashboard.privacyBoundary}</p></div><div className="grid gap-4 md:grid-cols-2">{dashboard.controls.map((control) => <article key={control.name} className="rounded-2xl border border-slate-100 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm"><div className="flex items-center justify-between gap-3"><h3 className="font-semibold text-slate-800">{control.name}</h3><span className="inline-flex items-center gap-1 rounded-full bg-[#E6F4EA] px-2.5 py-1 text-[10px] font-extrabold tracking-wide text-[#137333]"><CheckCircle2 className="h-3.5 w-3.5" />{control.status}</span></div><p className="mt-3 text-sm leading-6 text-slate-500">{control.detail}</p></article>)}</div><p className="mt-7 text-xs text-slate-400">Control status checked {new Date(dashboard.checkedAt).toLocaleString()}.</p></div>
  </section>;
}
