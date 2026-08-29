import { useState } from 'react';
import { Loader2, MessageCircleHeart, Send } from 'lucide-react';
import { api } from '../../api/client';
import { EmptyState } from '../common/EmptyState';
import { RagReply } from './RagReply';

export function RagView({ setError }) {
  const [query, setQuery] = useState('');
  const [messages, setMessages] = useState([]);
  const [asking, setAsking] = useState(false);

  const ask = async (event) => {
    event.preventDefault();
    const value = query.trim();
    if (!value || asking) return;
    setAsking(true);
    setError('');
    setMessages((current) => [...current, { role: 'user', text: value }]);
    setQuery('');
    try {
      const result = await api('/api/chat/rag', { method: 'POST', body: JSON.stringify({ query: value }) });
      setMessages((current) => [...current, { role: 'assistant', ...result }]);
    } catch (requestError) {
      setMessages((current) => current.slice(0, -1));
      setError(requestError.message);
    } finally {
      setAsking(false);
    }
  };

  return <section className="mx-auto flex min-h-[600px] max-w-4xl flex-col overflow-hidden rounded-3xl border border-[#E8E6E0] bg-white/60"><div className="border-b border-[#E8E6E0] p-6 sm:p-8"><div className="flex items-center gap-3"><div className="grid h-11 w-11 place-items-center rounded-xl bg-[#E8EBE8] text-[#7A8D80]"><MessageCircleHeart className="h-5 w-5" /></div><div><h2 className="font-serif text-2xl font-semibold">Chat with Past Self</h2><p className="mt-1 text-sm text-[#9A968D]">Grounded only in your own private reflections.</p></div></div></div><div className="flex-1 space-y-5 p-5 sm:p-8">{!messages.length && <EmptyState icon={<MessageCircleHeart className="h-7 w-7" />} title="Ask your past self" description="Try: When did I feel most productive last week?" />}{messages.map((message, index) => message.role === 'user' ? <div key={index} className="ml-auto max-w-[85%] rounded-2xl rounded-tr-none bg-[#7A8D80] px-5 py-4 text-[15px] leading-relaxed text-white">{message.text}</div> : <RagReply key={index} message={message} />)}{asking && <div className="flex items-center gap-2 text-sm text-[#9A968D]"><Loader2 className="h-4 w-4 animate-spin" />Searching your memories...</div>}</div><form onSubmit={ask} className="border-t border-[#E8E6E0] p-4 sm:p-5"><div className="flex gap-3"><input value={query} onChange={(event) => setQuery(event.target.value)} maxLength={4000} className="min-w-0 flex-1 rounded-xl border border-[#DCDCD2] bg-white px-4 py-3 text-sm placeholder:text-[#9A968D]" placeholder="Ask about a memory, habit, or feeling..." /><button disabled={asking || !query.trim()} className="grid h-11 w-11 place-items-center rounded-xl bg-[#7A8D80] text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50" aria-label="Ask past self"><Send className="h-4 w-4" /></button></div></form></section>;
}
