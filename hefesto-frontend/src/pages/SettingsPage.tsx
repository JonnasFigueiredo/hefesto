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
          CONFIGURAÇÕES
        </h1>
        <Badge variant="dim">USO INTERNO</Badge>
      </div>

      <Frame title="// SISTEMA">
        <div className="grid grid-cols-2 gap-3 font-mono text-[12px]">
          <div className="text-[var(--text-muted)]">STATUS DO BACKEND</div>
          <div className="flex items-center gap-2">
            <StatusDot status={status} />
            <span className="text-[var(--text)]">{status.toUpperCase()}</span>
          </div>
          <div className="text-[var(--text-muted)]">SERVIÇO</div>
          <div className="text-[var(--text)]">{data?.service ?? '—'}</div>
          <div className="text-[var(--text-muted)]">VERSÃO</div>
          <div className="text-[var(--text)]">{data?.version ?? '—'}</div>
        </div>
      </Frame>

      <Frame title="// ADAPTERS LLM">
        <Panel className="font-mono text-[11px] text-[var(--text-dim)]">
          // CONFIGURADO VIA application-local.yml
        </Panel>
      </Frame>

      <Frame title="// CONEXÃO JIRA">
        <div className="space-y-3">
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              URL DO JIRA
            </label>
            <Input mono placeholder="https://suaempresa.atlassian.net" disabled />
          </div>
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              EMAIL
            </label>
            <Input mono placeholder="voce@empresa.com" disabled />
          </div>
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              API TOKEN
            </label>
            <Input mono type="password" placeholder="************" disabled />
          </div>
          <div className="pt-2">
            <Button variant="primary" disabled>
              TESTAR CONEXÃO
            </Button>
            <span className="ml-3 font-mono text-[10px] text-[var(--text-muted)]">
              // EDITE application-local.yml E REINICIE O BACKEND
            </span>
          </div>
        </div>
      </Frame>

      <Frame title="// APARÊNCIA">
        <Panel className="font-mono text-[11px] text-[var(--text-dim)]">
          // SCANLINE / INTENSIDADE DE GLOW / CRT — EM BREVE
        </Panel>
      </Frame>
    </div>
  )
}
