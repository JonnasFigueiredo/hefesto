import { Badge } from '@/components/ui/Badge'
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
  if (!status) return <Badge variant="dim">DESCONHECIDO</Badge>

  const variant =
    status.category === 'done'
      ? 'green'
      : status.category === 'indeterminate'
        ? 'amber'
        : 'dim'

  return <Badge variant={variant}>{status.name}</Badge>
}
