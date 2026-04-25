import { Frame } from '@/components/ui/Frame'
import { Panel } from '@/components/ui/Panel'
import { Badge } from '@/components/ui/Badge'
import { StatusDot } from '@/components/ui/StatusDot'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useHealth } from '@/hooks/useHealth'

export function SettingsPage() {
  const { data, isError, isLoading } = useHealth()
  const status = isLoading ? 'pending' : isError ? 'offline' : 'online'

  return (
    <div className="h-full flex flex-col gap-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          SETTINGS
        </h1>
        <Badge variant="dim">STAGE 1 // STUB</Badge>
      </div>

      <Frame title="// SYSTEM">
        <div className="grid grid-cols-2 gap-3 font-mono text-[12px]">
          <div className="text-[var(--text-muted)]">BACKEND STATUS</div>
          <div className="flex items-center gap-2">
            <StatusDot status={status} />
            <span className="text-[var(--text)]">{status.toUpperCase()}</span>
          </div>
          <div className="text-[var(--text-muted)]">SERVICE</div>
          <div className="text-[var(--text)]">{data?.service ?? '—'}</div>
          <div className="text-[var(--text-muted)]">VERSION</div>
          <div className="text-[var(--text)]">{data?.version ?? '—'}</div>
        </div>
      </Frame>

      <Frame title="// LLM ADAPTERS">
        <Panel className="font-mono text-[11px] text-[var(--text-dim)]">
          // ADAPTER REGISTRY ACTIVATES IN STAGE 2
        </Panel>
      </Frame>

      <Frame title="// JIRA CONNECTION">
        <div className="space-y-3">
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              JIRA URL
            </label>
            <Input mono placeholder="https://yourcompany.atlassian.net" disabled />
          </div>
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              EMAIL
            </label>
            <Input mono placeholder="you@company.com" disabled />
          </div>
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              API TOKEN
            </label>
            <Input mono type="password" placeholder="************" disabled />
          </div>
          <div className="pt-2">
            <Button variant="primary" disabled>
              TEST CONNECTION
            </Button>
            <span className="ml-3 font-mono text-[10px] text-[var(--text-muted)]">
              // ENABLED IN STAGE 4
            </span>
          </div>
        </div>
      </Frame>

      <Frame title="// APPEARANCE">
        <Panel className="font-mono text-[11px] text-[var(--text-dim)]">
          // SCANLINE / GLOW INTENSITY / CRT TOGGLE — COMING SOON
        </Panel>
      </Frame>
    </div>
  )
}
