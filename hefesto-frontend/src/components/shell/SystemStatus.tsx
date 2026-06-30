import { useHealth } from '@/hooks/useHealth'
import { useJiraStatus } from '@/hooks/useJira'
import { StatusDot } from '@/components/ui/StatusDot'
import { useTranslation } from '@/i18n/I18nProvider'

export function SystemStatus() {
  const { data, isError, isLoading } = useHealth()
  const jira = useJiraStatus()
  const { t } = useTranslation()

  const status = isLoading ? 'pending' : isError ? 'offline' : 'online'
  const label = isLoading
    ? t('status.connecting')
    : isError
      ? t('status.offline')
      : t('status.online')

  const jiraStatus = jira.isLoading ? 'pending' : jira.data?.configured ? 'online' : 'offline'
  const jiraLabel = jira.isLoading
    ? t('jira.checking')
    : jira.data?.configured
      ? t('jira.connected')
      : t('status.jiraOff')

  return (
    <div className="border-t border-[var(--border-dim)] px-5 py-4 space-y-2 font-mono text-[10px]">
      <div className="flex items-center gap-2">
        <StatusDot status={status} />
        <span className="text-[var(--text-dim)]">{t('status.backend')}</span>
        <span className="ml-auto text-[var(--text)]">{label}</span>
      </div>
      <div className="flex items-center gap-2">
        <StatusDot status={jiraStatus} />
        <span className="text-[var(--text-dim)]">{t('status.jira')}</span>
        <span className="ml-auto text-[var(--text)]">{jiraLabel}</span>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-[var(--text-muted)]">{t('status.version')}</span>
        <span className="ml-auto text-[var(--text-dim)]">{data?.version ?? '0.1.0'}</span>
      </div>
    </div>
  )
}
