export function formatDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor(totalSeconds % 3600 / 60)
  const seconds = totalSeconds % 60
  return hours > 0
    ? `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
    : `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export function formatViews(value: string): string {
  const views = Number(value)
  if (!Number.isFinite(views)) return value
  if (views >= 10_000) return `${(views / 10_000).toFixed(1).replace(/\.0$/, '')} 万`
  return views.toLocaleString('zh-CN')
}

export function formatPublishedAt(value: string): string {
  const elapsedHours = Math.max(0, (Date.now() - new Date(value).getTime()) / 3_600_000)
  if (elapsedHours < 1) return '刚刚'
  if (elapsedHours < 24) return `${Math.floor(elapsedHours)} 小时前`
  if (elapsedHours < 24 * 7) return `${Math.floor(elapsedHours / 24)} 天前`
  return new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(new Date(value))
}
