import { Badge } from '@/components/ui/Badge'
import { useTranslation } from '@/i18n/I18nProvider'
import type { JiraStatus } from '@/types/jira'

interface Props {
  status: JiraStatus | null
}

/**
 * Mapeia categoria do status do Jira pra cor do Badge:
 *  - new           → cinza/dim
 *  - indeterminate → amber (in progress, in review, etc.)
 *  - done          → verde
 */
export function JiraStatusBadge({ status }: Props) {
  const { t } = useTranslation()
  if (!status) return <Badge variant="dim">{t('issues.statusUnknown')}</Badge>

  const variant =
    status.category === 'done'
      ? 'green'
      : status.category === 'indeterminate'
        ? 'amber'
        : 'dim'

  return <Badge variant={variant}>{status.name}</Badge>
}
