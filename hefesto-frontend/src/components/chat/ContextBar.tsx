import { useState } from 'react'
import { GitBranch, Plus, Sparkles, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { AttachmentsList } from './AttachmentsList'
import { AttachmentPicker } from './AttachmentPicker'
import { AgentSelector } from './AgentSelector'
import { useAgents } from '@/hooks/useAgents'
import { useActiveConversation, useChatStore } from '@/store/chatStore'
import { useTranslation } from '@/i18n/I18nProvider'

/**
 * Faixa logo abaixo do header da ChatWindow que sumariza o contexto da
 * conversa: agente selecionado, anexos, issue Jira anexada. Cada chip é
 * removível (exceto o agente, que pode ser trocado, não removido).
 */
export function ContextBar() {
  const conv = useActiveConversation()
  const { data: agents } = useAgents()
  const setJiraIssue = useChatStore((s) => s.setJiraIssue)
  const [editingKey, setEditingKey] = useState(false)
  const [keyInput, setKeyInput] = useState('')
  const { t } = useTranslation()

  if (!conv) return null

  const ctx = conv.context
  const agent = agents?.find((a) => a.id === ctx.agentId)
  const agentIsCustom = agent && agent.id !== 'default'

  return (
    <div className="flex flex-col gap-2 px-4 py-3 border-t border-[var(--border-dim)] bg-[rgba(0,212,255,0.02)]">
      {/* Linha 1: agente + ações */}
      <div className="flex items-center gap-2 flex-wrap">
        <AgentSelector />

        <AttachmentPicker />

        {/* Jira issue chip */}
        {ctx.jiraIssueKey ? (
          <Link
            to="/jira"
            className="group inline-flex items-center gap-2 px-2 h-7 border border-[var(--accent-amber)] text-[var(--accent-amber)] hover:bg-[rgba(255,181,71,0.06)] transition-colors"
            title={t('jira.openTooltip', { key: ctx.jiraIssueKey })}
          >
            <GitBranch size={12} strokeWidth={1.5} />
            <span className="font-mono text-[11px] tracking-[0.05em] font-bold">
              {ctx.jiraIssueKey}
            </span>
            <button
              onClick={(e) => {
                e.preventDefault()
                e.stopPropagation()
                setJiraIssue(conv.id, null)
              }}
              aria-label={t('jira.detachAria')}
              className="text-[var(--accent-amber)] hover:text-[var(--accent-magenta)]"
            >
              <X size={12} strokeWidth={2} />
            </button>
          </Link>
        ) : editingKey ? (
          <form
            onSubmit={(e) => {
              e.preventDefault()
              const k = keyInput.trim().toUpperCase()
              if (k) setJiraIssue(conv.id, k)
              setKeyInput('')
              setEditingKey(false)
            }}
            className="inline-flex items-center gap-1 h-7 border border-[var(--accent-amber)]"
          >
            <input
              autoFocus
              value={keyInput}
              onChange={(e) => setKeyInput(e.target.value)}
              onBlur={() => {
                setEditingKey(false)
                setKeyInput('')
              }}
              onKeyDown={(e) => {
                if (e.key === 'Escape') {
                  setEditingKey(false)
                  setKeyInput('')
                }
              }}
              placeholder="PROJ-123"
              className="bg-transparent w-[100px] px-2 font-mono text-[11px] text-[var(--accent-amber)] placeholder:text-[var(--text-muted)] focus:outline-none"
            />
          </form>
        ) : (
          <button
            onClick={() => setEditingKey(true)}
            className="inline-flex items-center gap-2 px-2 h-7 border border-[var(--border)] hover:border-[var(--accent-amber)] hover:text-[var(--accent-amber)] text-[var(--text-dim)] transition-colors font-display uppercase tracking-[0.15em] text-[11px]"
            title={t('jira.attachByKeyTooltip')}
          >
            <Plus size={12} strokeWidth={1.5} />
            {t('jira.attachByKey')}
          </button>
        )}

        {/* Resumo do agente ativo (quando custom) */}
        {agentIsCustom && agent && (
          <div className="ml-auto flex items-center gap-2 font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
            <Sparkles size={11} strokeWidth={1.5} className="text-[var(--accent-cyan)]" />
            <span>{t('agent.personaActive')}</span>
          </div>
        )}
      </div>

      {/* Linha 2: lista de arquivos anexados */}
      {ctx.attachmentIds.length > 0 && <AttachmentsList />}
    </div>
  )
}
