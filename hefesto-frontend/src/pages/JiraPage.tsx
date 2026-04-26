import { useState } from 'react'
import { Badge } from '@/components/ui/Badge'
import { Frame } from '@/components/ui/Frame'
import { JqlSearchBar } from '@/components/jira/JqlSearchBar'
import { IssueList } from '@/components/jira/IssueList'
import { IssueDetail } from '@/components/jira/IssueDetail'
import { BeetleMascot } from '@/components/jira/BeetleMascot'
import { useJiraIssue, useJiraSearch, useJiraStatus } from '@/hooks/useJira'
import { Link, useNavigate } from 'react-router-dom'
import { useChatStore } from '@/store/chatStore'
import type { JiraIssue } from '@/types/jira'

const DEFAULT_JQL = 'assignee = currentUser() ORDER BY updated DESC'

export function JiraPage() {
  const navigate = useNavigate()
  const [jql, setJql] = useState<string>(DEFAULT_JQL)
  const [activeKey, setActiveKey] = useState<string | null>(null)

  const status = useJiraStatus()
  const search = useJiraSearch(jql, status.data?.configured === true)
  const issue = useJiraIssue(activeKey)

  const isConfigured = status.data?.configured === true

  const handleSendToChat = (target: JiraIssue) => {
    const state = useChatStore.getState()
    const adapterId = state.selectedAdapterId ?? 'claude-code'
    state.createConversation(adapterId, {
      jiraIssueKey: target.key,
    })
    navigate('/chat')
  }

  return (
    <div className="h-full flex flex-col gap-4 relative">
      {/* Beetle decorativo no canto */}
      <div
        aria-hidden
        className="absolute top-0 right-0 opacity-25 hover:opacity-60 transition-opacity pointer-events-none"
        style={{ filter: 'drop-shadow(0 0 8px rgba(0, 212, 255, 0.15))' }}
      >
        <BeetleMascot size={90} />
      </div>

      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          JIRA
        </h1>
        <Badge variant="cyan">STAGE 4 // READ</Badge>
        {isConfigured ? (
          <Badge variant="green">CONNECTED</Badge>
        ) : status.isLoading ? (
          <Badge variant="dim">CHECKING...</Badge>
        ) : (
          <Badge variant="magenta">NOT CONFIGURED</Badge>
        )}
      </div>

      {!isConfigured && !status.isLoading && (
        <Frame variant="danger" title="// JIRA NOT CONFIGURED">
          <div className="font-sans text-[13px] text-[var(--text-dim)] mb-3">
            Configure as credenciais Jira em <code className="font-mono text-[12px] text-[var(--accent-cyan)]">application-local.yml</code> ou
            via env vars (<code className="font-mono text-[12px]">JIRA_URL</code>, <code className="font-mono text-[12px]">JIRA_EMAIL</code>, <code className="font-mono text-[12px]">JIRA_TOKEN</code>),
            depois reinicie o backend.
          </div>
          <Link
            to="/settings"
            className="inline-block font-display uppercase tracking-[0.15em] text-[12px] text-[var(--accent-cyan)] hover:underline"
          >
            // GO TO SETTINGS →
          </Link>
        </Frame>
      )}

      {isConfigured && (
        <div className="flex-1 grid grid-cols-[minmax(380px,2fr)_3fr] gap-4 min-h-0">
          {/* Coluna esquerda: busca + lista */}
          <Frame title="// QUERY" className="flex flex-col" padded={false}>
            <div className="p-4 border-b border-[var(--border-dim)]">
              <JqlSearchBar
                initialJql={jql}
                onExecute={(q) => {
                  setJql(q)
                  setActiveKey(null)
                }}
                isLoading={search.isFetching}
              />
            </div>
            <div className="flex-1 overflow-y-auto p-4">
              <IssueList
                issues={search.data?.issues ?? []}
                isLast={search.data?.isLast ?? true}
                activeKey={activeKey}
                onSelect={(k) => setActiveKey(k)}
                isLoading={search.isLoading}
                isError={search.isError}
                errorMessage={
                  search.error instanceof Error ? search.error.message : undefined
                }
              />
            </div>
          </Frame>

          {/* Coluna direita: detalhe */}
          <Frame title="// ISSUE" className="flex flex-col" padded={false}>
            <div className="p-5 flex-1 min-h-0 overflow-hidden">
              {!activeKey ? (
                <EmptyState />
              ) : issue.isLoading ? (
                <DetailLoading />
              ) : issue.isError ? (
                <DetailError
                  message={issue.error instanceof Error ? issue.error.message : 'failed'}
                />
              ) : issue.data ? (
                <IssueDetail issue={issue.data} onSendToChat={handleSendToChat} />
              ) : null}
            </div>
          </Frame>
        </div>
      )}
    </div>
  )
}

function EmptyState() {
  return (
    <div className="h-full flex flex-col items-center justify-center text-center gap-4 py-12">
      <BeetleMascot size={140} className="opacity-40" />
      <div className="font-mono text-[12px] text-[var(--text-dim)] uppercase tracking-[0.18em]">
        // SELECT AN ISSUE TO INSPECT
      </div>
      <div className="font-mono text-[10px] text-[var(--text-muted)] max-w-md">
        Use a barra de busca ou clique em uma das sugestões rápidas (MEUS, EM PROGRESSO, etc.)
      </div>
    </div>
  )
}

function DetailLoading() {
  return (
    <div className="space-y-4">
      <div className="h-8 w-32 bg-[var(--bg-overlay)] animate-shimmer" />
      <div className="h-4 w-3/4 bg-[var(--bg-overlay)] animate-shimmer" />
      <div className="h-4 w-1/2 bg-[var(--bg-overlay)] animate-shimmer" />
      <div className="border-t border-[var(--border-dim)] pt-4 space-y-2">
        <div className="h-3 bg-[var(--bg-overlay)] w-full" />
        <div className="h-3 bg-[var(--bg-overlay)] w-5/6" />
        <div className="h-3 bg-[var(--bg-overlay)] w-4/6" />
      </div>
    </div>
  )
}

function DetailError({ message }: { message: string }) {
  return (
    <div className="border border-[var(--accent-magenta)] p-4 font-mono text-[11px] text-[var(--accent-magenta)]">
      // ERROR // {message}
    </div>
  )
}
