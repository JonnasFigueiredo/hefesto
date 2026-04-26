import { useState } from 'react'
import { ExternalLink, Send } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Button } from '@/components/ui/Button'
import { JiraStatusBadge } from './JiraStatusBadge'
import { cn } from '@/lib/cn'
import type { JiraIssue } from '@/types/jira'

type Tab = 'description' | 'acceptance' | 'comments' | 'meta'

interface Props {
  issue: JiraIssue
  onSendToChat?: (issue: JiraIssue) => void
}

const TABS: { id: Tab; label: string }[] = [
  { id: 'description', label: 'DESCRIPTION' },
  { id: 'acceptance', label: 'ACCEPTANCE' },
  { id: 'comments', label: 'COMMENTS' },
  { id: 'meta', label: 'META' },
]

function formatDate(ts: number): string {
  if (!ts) return '—'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function IssueDetail({ issue, onSendToChat }: Props) {
  const [tab, setTab] = useState<Tab>('description')

  return (
    <div className="flex flex-col gap-4 h-full overflow-hidden">
      {/* Header */}
      <div className="flex items-start gap-3">
        <div className="flex-1 min-w-0">
          <div className="font-display text-[28px] uppercase tracking-[0.15em] text-[var(--accent-cyan)] glow-cyan leading-none">
            {issue.key}
          </div>
          <div className="font-sans text-[15px] text-[var(--text)] mt-2">
            {issue.summary}
          </div>
          <div className="flex items-center gap-2 mt-2">
            <JiraStatusBadge status={issue.status} />
            {issue.issueType && (
              <span className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
                // {issue.issueType}
              </span>
            )}
            {issue.priority && (
              <span className="font-mono text-[10px] text-[var(--text-dim)]">
                {issue.priority}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Action bar */}
      <div className="flex items-center gap-2 border-t border-b border-[var(--border-dim)] py-2">
        <Button
          variant="primary"
          size="sm"
          icon={<Send size={12} strokeWidth={1.5} />}
          onClick={() => onSendToChat?.(issue)}
          disabled={!onSendToChat}
        >
          SEND TO CHAT
        </Button>
        {issue.url && (
          <a
            href={issue.url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 h-7 px-3 text-[11px] font-display uppercase tracking-[0.15em] border border-[var(--border)] text-[var(--text)] hover:border-[var(--accent-cyan)] hover:text-[var(--accent-cyan)] transition-colors"
          >
            <ExternalLink size={12} strokeWidth={1.5} />
            OPEN IN JIRA
          </a>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-[var(--border-dim)]">
        {TABS.map((t) => {
          const active = t.id === tab
          const count =
            t.id === 'acceptance'
              ? issue.acceptanceCriteria.length
              : t.id === 'comments'
                ? issue.comments.length
                : null
          return (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={cn(
                'px-3 py-2 font-display uppercase tracking-[0.15em] text-[11px] relative transition-colors',
                active
                  ? 'text-[var(--accent-cyan)]'
                  : 'text-[var(--text-dim)] hover:text-[var(--text)]',
              )}
            >
              <span>{t.label}</span>
              {count !== null && (
                <span className="ml-1 text-[var(--text-muted)]">[{count}]</span>
              )}
              {active && (
                <span
                  aria-hidden
                  className="absolute bottom-[-1px] left-0 right-0 h-[2px] bg-[var(--accent-cyan)]"
                  style={{ boxShadow: '0 0 8px rgba(0,212,255,0.6)' }}
                />
              )}
            </button>
          )
        })}
      </div>

      {/* Tab content */}
      <div className="flex-1 overflow-y-auto pr-2">
        {tab === 'description' && (
          <DescriptionTab markdown={issue.description} />
        )}
        {tab === 'acceptance' && (
          <AcceptanceTab criteria={issue.acceptanceCriteria} />
        )}
        {tab === 'comments' && (
          <CommentsTab issue={issue} />
        )}
        {tab === 'meta' && <MetaTab issue={issue} />}
      </div>
    </div>
  )
}

function DescriptionTab({ markdown }: { markdown: string | null }) {
  if (!markdown) {
    return (
      <div className="font-mono text-[11px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
        // NO DESCRIPTION
      </div>
    )
  }
  return (
    <div className="prose-hefesto text-[13px] text-[var(--text)] leading-relaxed">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: ({ children }) => <p className="mb-2">{children}</p>,
          h1: ({ children }) => (
            <h1 className="font-display uppercase tracking-[0.15em] text-[var(--accent-cyan)] text-lg mb-2 mt-3">
              {children}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 className="font-display uppercase tracking-[0.12em] text-[var(--accent-cyan)] text-base mb-2 mt-3">
              {children}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 className="font-display uppercase tracking-[0.12em] text-[var(--text)] text-sm mb-1 mt-2">
              {children}
            </h3>
          ),
          ul: ({ children }) => (
            <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>
          ),
          ol: ({ children }) => (
            <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol>
          ),
          a: ({ children, href }) => (
            <a href={href} target="_blank" rel="noopener noreferrer" className="text-[var(--accent-cyan)] underline hover:no-underline">
              {children}
            </a>
          ),
          code: ({ className, children, ...props }) => {
            const isInline = !className
            if (isInline) {
              return (
                <code className="font-mono text-[12px] px-1 py-[1px] bg-[var(--bg-overlay)] border border-[var(--border-dim)] text-[var(--accent-cyan)]" {...props}>
                  {children}
                </code>
              )
            }
            return <code className={cn('font-mono text-[12px]', className)} {...props}>{children}</code>
          },
          pre: ({ children }) => (
            <pre className="my-2 p-3 bg-[var(--bg-overlay)] border border-[var(--border-dim)] overflow-x-auto font-mono text-[12px] text-[var(--text)]">
              {children}
            </pre>
          ),
          blockquote: ({ children }) => (
            <blockquote className="border-l-2 border-[var(--accent-cyan)] pl-3 text-[var(--text-dim)] my-2 italic">
              {children}
            </blockquote>
          ),
        }}
      >
        {markdown}
      </ReactMarkdown>
    </div>
  )
}

function AcceptanceTab({ criteria }: { criteria: string[] }) {
  if (criteria.length === 0) {
    return (
      <div className="font-mono text-[11px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
        // NO ACCEPTANCE CRITERIA DETECTED
        <div className="mt-2 normal-case tracking-normal">
          {'(o backend procura por seções "critérios de aceite" / "acceptance criteria" na descrição)'}
        </div>
      </div>
    )
  }
  return (
    <ul className="space-y-2 font-sans text-[13px] text-[var(--text)]">
      {criteria.map((c, i) => (
        <li key={i} className="flex items-start gap-3">
          <span className="font-mono text-[12px] text-[var(--accent-cyan)] mt-[2px]">[ ]</span>
          <span>{c}</span>
        </li>
      ))}
    </ul>
  )
}

function CommentsTab({ issue }: { issue: JiraIssue }) {
  if (issue.comments.length === 0) {
    return (
      <div className="font-mono text-[11px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
        // NO COMMENTS
      </div>
    )
  }
  return (
    <div className="relative pl-6 space-y-4">
      {/* timeline cyan */}
      <span aria-hidden className="absolute left-2 top-2 bottom-2 w-px bg-[var(--accent-cyan)] opacity-40" />
      {issue.comments.map((c) => (
        <div key={c.id} className="relative">
          <span aria-hidden className="absolute -left-[18px] top-2 w-2 h-2 rounded-full bg-[var(--accent-cyan)]" style={{ boxShadow: '0 0 6px rgba(0,212,255,0.6)' }} />
          <div className="border border-[var(--border-dim)] p-3 bg-[var(--bg-elevated)]">
            <div className="flex items-center gap-2 mb-2 font-mono text-[10px] uppercase tracking-[0.15em]">
              <span className="font-display text-[12px] text-[var(--text)] tracking-normal normal-case">
                {c.author?.displayName ?? 'Unknown'}
              </span>
              <span className="text-[var(--text-muted)] ml-auto">
                {formatDate(c.created)}
              </span>
            </div>
            <div className="font-sans text-[12px] text-[var(--text-dim)] whitespace-pre-wrap leading-relaxed">
              {c.body}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

function MetaTab({ issue }: { issue: JiraIssue }) {
  const rows: { label: string; value: string }[] = [
    { label: 'KEY', value: issue.key },
    { label: 'TYPE', value: issue.issueType ?? '—' },
    { label: 'PRIORITY', value: issue.priority ?? '—' },
    { label: 'STATUS', value: issue.status?.name ?? '—' },
    { label: 'ASSIGNEE', value: issue.assignee?.displayName ?? '— UNASSIGNED' },
    { label: 'REPORTER', value: issue.reporter?.displayName ?? '—' },
    { label: 'SPRINT', value: issue.sprint ?? '—' },
    { label: 'LABELS', value: issue.labels.length ? issue.labels.join(', ') : '—' },
    { label: 'CREATED', value: formatDate(issue.created) },
    { label: 'UPDATED', value: formatDate(issue.updated) },
  ]
  return (
    <table className="w-full font-mono text-[11px]">
      <tbody>
        {rows.map((r) => (
          <tr key={r.label} className="border-b border-[var(--border-dim)]">
            <td className="py-2 pr-4 text-[var(--text-muted)] uppercase tracking-[0.18em] w-32">
              {r.label}
            </td>
            <td className="py-2 text-[var(--text)]">{r.value}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
