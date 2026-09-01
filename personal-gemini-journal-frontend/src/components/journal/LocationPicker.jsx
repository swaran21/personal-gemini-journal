import { useState } from 'react';
import { Loader2, LocateFixed, MapPin, X } from 'lucide-react';

export function LocationPicker({ location, onChange, setError }) {
  const [locating, setLocating] = useState(false);
  const locate = () => {
    if (!navigator.geolocation) { setError('Location is not supported by this browser.'); return; }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => { onChange({ latitude: Number(coords.latitude.toFixed(6)), longitude: Number(coords.longitude.toFixed(6)), label: '' }); setLocating(false); },
      () => { setError('Location permission was not granted. You can continue without adding a location.'); setLocating(false); },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 },
    );
  };

  if (!location) return <button type="button" onClick={locate} disabled={locating} className="mt-3 inline-flex items-center gap-2 rounded-xl border border-[#DCDCD2] bg-white px-3 py-2 text-xs font-semibold text-[#7A8D80] hover:bg-[#F9F8F4] disabled:opacity-60">{locating ? <Loader2 className="h-4 w-4 animate-spin" /> : <LocateFixed className="h-4 w-4" />}Add current location</button>;
  return <div className="mt-3 flex flex-col gap-3 rounded-2xl border border-[#DCDCD2] bg-[#F9F8F4] p-3 sm:flex-row sm:items-center"><MapPin className="h-4 w-4 shrink-0 text-[#B87D64]" /><input value={location.label} maxLength={200} onChange={(event) => onChange({ ...location, label: event.target.value })} className="min-w-0 flex-1 rounded-lg border border-[#DCDCD2] bg-white px-3 py-2 text-sm" placeholder="Optional place label, e.g. Campus library" /><span className="text-xs text-[#9A968D]">{location.latitude}, {location.longitude}</span><button type="button" onClick={() => onChange(null)} className="self-end rounded-full p-1 text-[#9A968D] hover:text-red-500" aria-label="Remove location"><X className="h-4 w-4" /></button></div>;
}
