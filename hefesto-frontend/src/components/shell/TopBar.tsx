import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { useClock, formatClock } from '@/hooks/useClock'
import { subscribeLatency, getLastLatency } from '@/api/http'

function pageLabel(pathname: string): string {
  if (pathname.startsWith('/chat')) return 'CHAT'
  if (pathname.startsWith('/jira')) return 'JIRA'
  if (pathname.startsWith('/settings')) return 'SETTINGS'
  return 'HOME'
}

export function TopBar() {
  const now = useClock()
  const { pathname } = useLocation()
  const [latency, setLatency] = useState<number | null>(getLastLatency())

  useEffect(() => subscribeLatency(setLatency), [])

  return (
    <header className="h-10 shrink-0 border-b border-[var(--border-dim)] flex items-center px-5 gap-4 font-mono text-[10px] text-[var(--text-dim)] uppercase tracking-[0.15em]">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2">
        <span className="text-[var(--text-muted)]">HEFESTO</span>
        <span className="text-[var(--text-muted)]">/</span>
        <span className="text-[var(--accent-cyan)]">{pageLabel(pathname)}</span>
      </div>

      {/* Right side */}
      <div className="ml-auto flex items-center gap-5">
        <div className="flex items-center gap-2">
          <span className="text-[var(--text-muted)]">LAT</span>
          <span className="text-[var(--text)]">
            {latency === null ? '— ms' : `${latency} ms`}
          </span>
        </div>
        <div className="text-[var(--text)]">{formatClock(now)}</div>
      </div>
    </header>
  )
}
