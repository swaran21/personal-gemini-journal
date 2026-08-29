import { useState } from 'react';
import { Bot, ChevronDown, ChevronUp } from 'lucide-react';

export function RagReply({ message }) {
  const [open, setOpen] = useState(false);
  return <div className="max-w-[92%] rounded-2xl rounded-tl-none border border-[#E8E6E0] bg-white p-5 text-[15px] leading-relaxed text-[#3A3A35]"><div className="mb-2 flex items-center gap-2 text-[11px] font-bold uppercase tracking-[.16em] text-[#7A8D80]"><Bot className="h-3.5 w-3.5" />Past Self</div><p className="whitespace-pre-wrap">{message.reply}</p>{message.referencedEntries?.length > 0 && <div className="mt-4 border-t border-[#E8E6E0] pt-3"><button onClick={() => setOpen(!open)} className="flex w-full items-center justify-between text-xs font-medium text-[#7A8D80]">Referenced memories <span className="flex items-center gap-1">{message.referencedEntries.length}{open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}</span></button>{open && <ul className="mt-3 space-y-2">{message.referencedEntries.map((reference, index) => <li key={`${index}-${reference}`} className="whitespace-pre-wrap rounded-lg bg-[#F9F8F4] px-3 py-2 text-xs leading-relaxed text-[#7A756C]">{reference}</li>)}</ul>}</div>}</div>;
}
