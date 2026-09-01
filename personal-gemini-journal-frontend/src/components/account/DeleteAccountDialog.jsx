import { useState } from 'react';
import { AlertTriangle, Loader2, X } from 'lucide-react';
import { api } from '../../api/client';
import { authProvider } from '../../auth/authProvider';

export function DeleteAccountDialog({ onClose }) {
  const [confirmation, setConfirmation] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');
  const canDelete = confirmation === 'DELETE';

  const removeAccount = async (event) => {
    event.preventDefault();
    if (!canDelete || deleting) return;
    setDeleting(true);
    setError('');
    try {
      await api('/api/account', { method: 'DELETE' });
      await authProvider.logout();
    } catch (requestError) {
      setError(requestError.message);
      setDeleting(false);
    }
  };

  return <div className="fixed inset-0 z-50 grid place-items-center bg-[#2D362E]/40 p-4 backdrop-blur-sm" role="presentation">
    <section role="dialog" aria-modal="true" aria-labelledby="delete-account-title" className="w-full max-w-lg rounded-3xl border border-[#E8C8BC] bg-[#FFFDFC] p-6 shadow-2xl sm:p-8">
      <div className="flex items-start justify-between gap-4"><div className="grid h-11 w-11 place-items-center rounded-2xl bg-[#F9E8E1] text-[#A45D43]"><AlertTriangle className="h-5 w-5" /></div><button type="button" onClick={onClose} disabled={deleting} className="rounded-full p-2 text-[#9A968D] hover:bg-[#F5F1ED]" aria-label="Close"><X className="h-5 w-5" /></button></div>
      <h2 id="delete-account-title" className="mt-5 font-serif text-2xl font-semibold text-[#2D362E]">Delete your journal account?</h2>
      <p className="mt-3 text-sm leading-relaxed text-[#6F716A]">This permanently deletes your journal entries, AI reflections, embeddings, processing jobs, and action items. This cannot be undone.</p>
      {authProvider.mode === 'oidc' && <p className="mt-3 rounded-xl bg-[#F9F8F4] p-3 text-xs leading-relaxed text-[#7A756C]">Local development uses Keycloak as an external identity provider. Your journal data is deleted here; the Keycloak login record remains manageable in the Keycloak admin console.</p>}
      <form onSubmit={removeAccount} className="mt-6"><label htmlFor="delete-confirmation" className="text-sm font-medium text-[#3A3A35]">Type <strong>DELETE</strong> to confirm</label><input id="delete-confirmation" value={confirmation} onChange={(event) => setConfirmation(event.target.value)} autoComplete="off" className="mt-2 w-full rounded-xl border border-[#DCDCD2] bg-white px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-[#A45D43]/20" />{error && <p className="mt-3 text-sm text-[#A45D43]" role="alert">{error}</p>}<div className="mt-6 flex justify-end gap-3"><button type="button" onClick={onClose} disabled={deleting} className="rounded-xl border border-[#DCDCD2] px-4 py-2.5 text-sm font-medium text-[#6F716A] hover:bg-[#F9F8F4]">Cancel</button><button type="submit" disabled={!canDelete || deleting} className="inline-flex items-center gap-2 rounded-xl bg-[#A45D43] px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40">{deleting && <Loader2 className="h-4 w-4 animate-spin" />}Delete permanently</button></div></form>
    </section>
  </div>;
}
