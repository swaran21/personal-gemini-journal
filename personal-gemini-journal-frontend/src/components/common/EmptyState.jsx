export function EmptyState({ icon, title, description }) {
  return <div className="grid min-h-48 place-items-center rounded-2xl border border-dashed border-[#DCDCD2] bg-white/50 p-8 text-center"><div><div className="mx-auto mb-3 text-[#9A968D]">{icon}</div><h3 className="font-medium text-[#2D362E]">{title}</h3><p className="mt-2 max-w-sm text-sm leading-relaxed text-[#9A968D]">{description}</p></div></div>;
}
