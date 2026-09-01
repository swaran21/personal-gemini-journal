import { useState } from 'react';
import { Download, FileJson, FileText, Loader2, X } from 'lucide-react';
import { download } from '../../api/client';

export function DataExportDialog({ onClose, setError }) {
  const [active, setActive] = useState('');
  const takeout = async (format) => {
    setActive(format); setError('');
    try { await download(`/api/user/export?format=${format}`); onClose(); }
    catch (requestError) { setError(requestError.message); }
    finally { setActive(''); }
  };
  return <div className="fixed inset-0 z-50 grid place-items-center bg-[#2D362E]/40 p-6 backdrop-blur-sm" role="dialog" aria-modal="true" aria-labelledby="export-title"><section className="w-full max-w-md rounded-3xl border border-[#E8E6E0] bg-white p-7 shadow-xl"><div className="flex items-start justify-between"><div className="grid h-11 w-11 place-items-center rounded-xl bg-[#E8EBE8] text-[#7A8D80]"><Download className="h-5 w-5" /></div><button type="button" onClick={onClose} className="rounded-full p-2 text-[#9A968D] hover:bg-[#F9F8F4]" aria-label="Close"><X className="h-5 w-5" /></button></div><h2 id="export-title" className="mt-5 font-serif text-2xl font-semibold">Download your data</h2><p className="mt-2 text-sm leading-relaxed text-[#7A756C]">Your journal entries, AI reflections, approved locations, and action items are compiled securely. Embeddings and internal processing records are excluded.</p><div className="mt-6 grid gap-3"><button type="button" onClick={() => takeout('json')} disabled={active} className="flex items-center gap-3 rounded-xl border border-[#DCDCD2] px-4 py-3 text-left hover:bg-[#F9F8F4]"><FileJson className="h-5 w-5 text-[#7A8D80]" /><span className="flex-1"><strong className="block text-sm">JSON archive</strong><span className="text-xs text-[#9A968D]">Best for backups and portability</span></span>{active === 'json' && <Loader2 className="h-4 w-4 animate-spin" />}</button><button type="button" onClick={() => takeout('markdown')} disabled={active} className="flex items-center gap-3 rounded-xl border border-[#DCDCD2] px-4 py-3 text-left hover:bg-[#F9F8F4]"><FileText className="h-5 w-5 text-[#7A8D80]" /><span className="flex-1"><strong className="block text-sm">Markdown journal</strong><span className="text-xs text-[#9A968D]">Easy to read in any text editor</span></span>{active === 'markdown' && <Loader2 className="h-4 w-4 animate-spin" />}</button></div></section></div>;
}
