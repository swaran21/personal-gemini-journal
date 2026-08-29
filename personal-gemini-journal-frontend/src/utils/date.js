export function dayLabel(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Today' : date.toLocaleString(undefined, { weekday: 'long', month: 'short', day: 'numeric' });
}

export function timeLabel(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Just now' : date.toLocaleString(undefined, { hour: 'numeric', minute: '2-digit' });
}
