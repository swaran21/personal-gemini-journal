import { X } from 'lucide-react';

export function ErrorBanner({ message, onClose }) {
  return <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><X className="mt-0.5 h-4 w-4 shrink-0" /><p className="flex-1">{message}</p><button onClick={onClose} aria-label="Dismiss error">x</button></div>;
}
