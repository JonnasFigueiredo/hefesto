import { Frame } from '@/components/ui/Frame'
import { Panel } from '@/components/ui/Panel'
import { Badge } from '@/components/ui/Badge'
import { StatusDot } from '@/components/ui/StatusDot'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useTranslation } from '@/i18n/I18nProvider'
import { useHealth } from '@/hooks/useHealth'

export function SettingsPage() {
  const { t } = useTranslation()
  const { data, isError, isLoading } = useHealth()
  const status = isLoading ? 'pending' : isError ? 'offline' : 'online'

  return (
    <div className="h-full flex flex-col gap-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          {t('settings.title')}
        </h1>
        <Badge variant="dim">{t('settings.badgeInternal')}</Badge>
      </div>

      <Frame title={t('settings.system')}>
        <div className="grid grid-cols-2 gap-3 font-mono text-[12px]">
          <div className="text-[var(--text-muted)]">{t('settings.statusBackend')}</div>
          <div className="flex items-center gap-2">
            <StatusDot status={status} />
            <span className="text-[var(--text)]">{status.toUpperCase()}</span>
          </div>
          <div className="text-[var(--text-muted)]">{t('settings.service')}</div>
          <div className="text-[var(--text)]">{data?.service ?? '—'}</div>
          <div className="text-[var(--text-muted)]">{t('settings.version')}</div>
          <div className="text-[var(--text)]">{data?.version ?? '—'}</div>
        </div>
      </Frame>

      <Frame title={t('settings.adapters')}>
        <Panel className="font-mono text-[11px] text-[var(--text-dim)]">
          {t('settings.adaptersHint')}
        </Panel>
      </Frame>

      <Frame title={t('settings.jira')}>
        <div className="space-y-3">
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              {t('settings.jiraUrl')}
            </label>
            <Input mono placeholder="https://suaempresa.atlassian.net" disabled />
          </div>
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              {t('settings.email')}
            </label>
            <Input mono placeholder="voce@empresa.com" disabled />
          </div>
          <div>
            <label className="block font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-wider mb-1">
              {t('settings.token')}
            </label>
            <Input mono type="password" placeholder="************" disabled />
          </div>
          <div className="pt-2">
            <Button variant="primary" disabled>
              {t('settings.testConnection')}
            </Button>
            <span className="ml-3 font-mono text-[10px] text-[var(--text-muted)]">
              {t('settings.testHint')}
            </span>
          </div>
        </div>
      </Frame>

      <Frame title={t('settings.appearance')}>
        <Panel className="font-mono text-[11px] text-[var(--text-dim)]">
          {t('settings.appearanceHint')}
        </Panel>
      </Frame>
    </div>
  )
}
