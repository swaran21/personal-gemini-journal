import { useState } from 'react';
import { BookHeart, Loader2, LogIn } from 'lucide-react';
import { authProvider } from '../../auth/authProvider';

export function LoginScreen({ initialError = '' }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(initialError);
  const login = async () => { setLoading(true); setError(''); try { await authProvider.login(); } catch (signInError) { setError(signInError.message.replace('Firebase: ', '')); } finally { setLoading(false); } };
  return <div className="flex min-h-screen items-center justify-center bg-[#F9F8F4] p-6"><section className="w-full max-w-md rounded-3xl border border-[#DCDCD2] bg-white p-8 text-center shadow-sm"><div className="mx-auto mb-6 grid h-16 w-16 place-items-center rounded-2xl bg-[#7A8D80] text-white"><BookHeart className="h-8 w-8" /></div><h1 className="font-serif text-2xl font-semibold tracking-tight text-[#2D362E]">Gemini Journal</h1><p className="mt-3 leading-relaxed text-[#9A968D]">A private, secure space for reflection. Powered by AI to help you find clarity and track actionable goals.</p>{error && <p className="mt-5 rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}<button onClick={login} disabled={loading} className="mt-8 flex w-full items-center justify-center gap-3 rounded-xl bg-[#7A8D80] px-6 py-3.5 font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50">{loading ? <Loader2 className="h-5 w-5 animate-spin" /> : <LogIn className="h-5 w-5" />}{loading ? 'Signing in...' : authProvider.loginLabel}</button></section></div>;
}
