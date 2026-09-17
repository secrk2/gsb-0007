export const STATUS_LABELS = {
  REGISTERED: '注册告知',
  ACCEPTED: '验收',
  IN_USE: '在用',
  SUSPENDED: '停用',
  SCRAPPED: '报废'
}

export const TYPE_LABELS = { ELEVATOR: '电梯', CRANE: '起重机械' }

export function fmtDate(s) {
  if (!s) return '—'
  return String(s).slice(0, 10)
}

export function fmtTime(s) {
  if (!s) return '—'
  return String(s).slice(0, 16).replace('T', ' ')
}

export function fmtClock(ts) {
  const d = new Date(ts)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function uuid() {
  return crypto.randomUUID()
}
