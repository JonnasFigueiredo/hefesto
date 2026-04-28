import { IssueCard } from './IssueCard'
import type { JiraIssueListItem } from '@/types/jira'

interface Props {
  issues: JiraIssueListItem[]
  isLast?: boolean
  activeKey?: string | null
  onSelect: (key: string) => void
  isLoading?: boolean
  isError?: boolean
  errorMessage?: string
}

export function IssueList({
  issues,
  isLast = true,
  activeKey,
  onSelect,
  isLoading,
  isError,
  errorMessage,
}: Props) {
  if (isLoading) {
    return <Skeletons />
  }

  if (isError) {
    return (
      <div className="border border-[var(--accent-magenta)] p-4 font-mono text-[11px] text-[var(--accent-magenta)]">
        // ERRO // {errorMessage ?? 'falha ao carregar histórias'}
      </div>
    )
  }

  if (issues.length === 0) {
    return (
      <div className="text-center py-12 font-mono text-[11px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
        // NENHUM RESULTADO
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.2em] mb-1 flex items-center gap-2">
        <span>// {issues.length} HISTÓRIA{issues.length === 1 ? '' : 'S'}</span>
        {!isLast && (
          <span className="text-[var(--accent-amber)]">// HÁ MAIS PÁGINAS</span>
        )}
      </div>
      {issues.map((issue) => (
        <IssueCard
          key={issue.key}
          issue={issue}
          active={issue.key === activeKey}
          onClick={() => onSelect(issue.key)}
        />
      ))}
    </div>
  )
}

function Skeletons() {
  return (
    <div className="flex flex-col gap-2">
      {Array.from({ length: 4 }).map((_, i) => (
        <div
          key={i}
          className="border border-[var(--border-dim)] p-3 relative overflow-hidden"
        >
          <div className="h-3 w-20 bg-[var(--bg-overlay)] mb-2" />
          <div className="h-4 w-full bg-[var(--bg-overlay)] mb-1" />
          <div className="h-4 w-3/4 bg-[var(--bg-overlay)] mb-2" />
          <div className="h-3 w-32 bg-[var(--bg-overlay)]" />
          <span
            aria-hidden
            className="absolute inset-0 pointer-events-none animate-shimmer"
            style={{
              background:
                'linear-gradient(90deg, transparent, rgba(0,212,255,0.06), transparent)',
              backgroundSize: '1000px 100%',
            }}
          />
        </div>
      ))}
    </div>
  )
}
