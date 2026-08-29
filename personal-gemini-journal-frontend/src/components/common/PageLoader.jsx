import { Loader2 } from 'lucide-react';

export function PageLoader({ fullScreen = false }) {
  return <div className={`grid place-items-center text-[#7A8D80] ${fullScreen ? 'min-h-screen bg-[#F9F8F4]' : 'min-h-48'}`}><Loader2 className="h-7 w-7 animate-spin" /></div>;
}
