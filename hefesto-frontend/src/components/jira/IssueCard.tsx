import { cn } from '@/lib/cn'
import { JiraStatusBadge } from './JiraStatusBadge'
import type { JiraIssueListItem } from '@/types/jira'

interface Props {
  issue: JiraIssueListItem
  active?: boolean
  onClick?: () => void
}

function timeAgo(ts: number): string {
  if (!ts) return '—'
  const diff = Date.now() - ts
  const m = Math.floor(diff / 60_000)
  if (m < 1) return 'now'
  if (m < 60) return `${m}m`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h`
  const d = Math.floor(h / 24)
  return `${d}d`
}

export function IssueCard({ issue, active = false, onClick }: Props) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'group w-full text-left relative border border-[var(--border-dim)] p-3 transition-all',
        'hover:border-[var(--accent-cyan)] hover:bg-[var(--bg-elevated)]',
        active && 'border-[var(--accent-cyan)] bg-[var(--bg-elevated)]',
      )}
    >
      {/* Corner brackets só quando ativo */}
      {active && (
        <>
          <span aria-hidden className="pointer-events-none absolute -top-px -left-px h-2 w-2 border-t border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -top-px -right-px h-2 w-2 border-t border-r border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -left-px h-2 w-2 border-b border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -right-px h-2 w-2 border-b border-r border-[var(--accent-cyan)]" />
        </>
      )}

      <div className="flex items-center gap-2 mb-1">
        <span className="font-mono text-[11px] font-bold text-[var(--accent-cyan)] tracking-[0.05em]">
          {issue.key}
        </span>
        {issue.issueType && (
          <span className="font-mono text-[9px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
            // {issue.issueType}
          </span>
        )}
        <span className="ml-auto">
          <JiraStatusBadge status={issue.status} />
        </span>
      </div>

      <div className="font-sans text-[13px] text-[var(--text)] mb-2 line-clamp-2">
        {issue.summary}
      </div>

      <div className="flex items-center gap-3 font-mono text-[10px] text-[var(--text-muted)]">
        <span className="truncate">
          {issue.assignee?.displayName ?? '— UNASSIGNED'}
        </span>
        {issue.priority && (
          <span className="text-[var(--text-dim)]">{issue.priority}</span>
        )}
        <span className="ml-auto">{timeAgo(issue.updated)}</span>
      </div>
    </button>
  )
}
