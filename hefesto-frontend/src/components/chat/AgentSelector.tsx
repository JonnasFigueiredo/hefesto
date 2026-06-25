import { useEffect, useRef, useState } from 'react'
import { ChevronDown, Check } from 'lucide-react'
import { useAgents } from '@/hooks/useAgents'
import { useActiveConversation, useChatStore } from '@/store/chatStore'
import { cn } from '@/lib/cn'
import { useTranslation } from '@/i18n/I18nProvider'

/**
 * Dropdown que lista agentes especialistas e permite selecionar um pra
 * conversa ativa. Ao selecionar, atualiza store.context.agentId.
 *
 * Modal com emoji grande, nome destacado, descrição completa e badge
 * indicando o agente ativo.
 */
export function AgentSelector() {
  const { data: agents, isLoading } = useAgents()
  const conv = useActiveConversation()
  const setAgent = useChatStore((s) => s.setAgent)
  const { t } = useTranslation()

  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const selectedId = conv?.context.agentId ?? 'default'
  const selected = agents?.find((a) => a.id === selectedId)

  if (isLoading) {
    return (
      <div className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em] px-3 py-1 border border-[var(--border-dim)]">
        {t('agent.loading')}
      </div>
    )
  }

  return (
    <div ref={ref} className="relative">
      {/* Trigger — mostra emoji grande + nome do agente atual */}
      <button
        onClick={() => setOpen((v) => !v)}
        disabled={!conv}
        className={cn(
          'flex items-center gap-2 px-3 h-8 border transition-colors',
          'border-[var(--border)] hover:border-[var(--accent-cyan)]',
          open && 'border-[var(--accent-cyan)] bg-[var(--bg-elevated)]',
          !conv && 'opacity-50 cursor-not-allowed',
        )}
        title={selected ? t('agent.titleTooltip', { name: selected.name }) : t('agent.selectFallback')}
      >
        <span className="text-[15px] leading-none">{selected?.emoji ?? '✦'}</span>
        <span className="font-display uppercase tracking-[0.15em] text-[11px] text-[var(--text)]">
          {selected?.name ?? t('agent.placeholder')}
        </span>
        <ChevronDown
          size={12}
          strokeWidth={1.5}
          className={cn('text-[var(--text-dim)] transition-transform ml-1', open && 'rotate-180')}
        />
      </button>

      {open && agents && (
        <div className="absolute bottom-10 right-0 z-50 w-[420px] bg-[var(--bg-base)] border border-[var(--accent-cyan)] shadow-[0_0_24px_rgba(0,212,255,0.18)]">
          {/* corner brackets */}
          <span aria-hidden className="pointer-events-none absolute -top-px -left-px h-3 w-3 border-t border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -top-px -right-px h-3 w-3 border-t border-r border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -left-px h-3 w-3 border-b border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -right-px h-3 w-3 border-b border-r border-[var(--accent-cyan)]" />

          {/* Header do modal */}
          <div className="px-4 py-3 border-b border-[var(--border-dim)] flex items-center justify-between">
            <div>
              <div className="font-display uppercase tracking-[0.18em] text-[12px] text-[var(--accent-cyan)] glow-cyan">
                {t('agent.modalTitle')}
              </div>
              <div className="font-mono text-[10px] text-[var(--text-muted)] mt-0.5">
                {t('agent.modalSubtitle')}
              </div>
            </div>
            <span className="font-mono text-[10px] text-[var(--text-muted)]">
              {agents.length} {t('agent.availableSuffix')}
            </span>
          </div>

          {/* Lista de cards */}
          <ul className="max-h-[480px] overflow-y-auto">
            {agents.map((a) => {
              const isSelected = a.id === selectedId
              return (
                <li key={a.id}>
                  <button
                    onClick={() => {
                      if (conv) setAgent(conv.id, a.id)
                      setOpen(false)
                    }}
                    className={cn(
                      'group w-full text-left px-4 py-3 flex items-start gap-4 transition-colors relative',
                      'hover:bg-[var(--bg-elevated)] border-b border-[var(--border-dim)] last:border-b-0',
                      isSelected && 'bg-[var(--bg-elevated)]',
                    )}
                  >
                    {/* Active marker à esquerda */}
                    {isSelected && (
                      <span
                        aria-hidden
                        className="absolute left-0 top-0 bottom-0 w-[2px] bg-[var(--accent-cyan)]"
                      />
                    )}

                    {/* Emoji em destaque */}
                    <div
                      className={cn(
                        'shrink-0 w-10 h-10 flex items-center justify-center text-[22px] border transition-colors',
                        isSelected
                          ? 'border-[var(--accent-cyan)] bg-[rgba(0,212,255,0.05)]'
                          : 'border-[var(--border-dim)] group-hover:border-[var(--border)]',
                      )}
                    >
                      {a.emoji ?? '✦'}
                    </div>

                    {/* Conteúdo */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span
                          className={cn(
                            'font-display uppercase tracking-[0.12em] text-[13px]',
                            isSelected ? 'text-[var(--accent-cyan)]' : 'text-[var(--text)]',
                          )}
                        >
                          {a.name}
                        </span>
                        {isSelected && (
                          <span className="inline-flex items-center gap-1 text-[9px] font-mono uppercase tracking-[0.18em] text-[var(--accent-cyan)] border border-[var(--accent-cyan)] px-1.5 py-[1px]">
                            <Check size={9} strokeWidth={2.5} />
                            {t('agent.active')}
                          </span>
                        )}
                      </div>
                      <div className="font-sans text-[12px] text-[var(--text-dim)] leading-snug">
                        {a.description}
                      </div>
                    </div>
                  </button>
                </li>
              )
            })}
          </ul>
        </div>
      )}
    </div>
  )
}
