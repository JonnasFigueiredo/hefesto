import { useHealth } from '@/hooks/useHealth'
import { StatusDot } from '@/components/ui/StatusDot'

export function SystemStatus() {
  const { data, isError, isLoading } = useHealth()

  const status = isLoading ? 'pending' : isError ? 'offline' : 'online'
  const label = isLoading ? 'CONNECTING' : isError ? 'OFFLINE' : 'ONLINE'

  return (
    <div className="border-t border-[var(--border-dim)] px-5 py-4 space-y-2 font-mono text-[10px]">
      <div className="flex items-center gap-2">
        <StatusDot status={status} />
        <span className="text-[var(--text-dim)]">BACKEND</span>
        <span className="ml-auto text-[var(--text)]">{label}</span>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-[var(--text-muted)]">ADAPTER</span>
        <span className="ml-auto text-[var(--text-dim)]">CLAUDE-CODE</span>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-[var(--text-muted)]">VERSION</span>
        <span className="ml-auto text-[var(--text-dim)]">{data?.version ?? '0.1.0'}</span>
      </div>
    </div>
  )
}
