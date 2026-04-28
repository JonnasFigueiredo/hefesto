import { useEffect, useRef, useState } from 'react'
import { ChevronDown, Sparkles } from 'lucide-react'
import { useAgents } from '@/hooks/useAgents'
import { useActiveConversation, useChatStore } from '@/store/chatStore'
import { cn } from '@/lib/cn'

/**
 * Dropdown que lista agentes especialistas e permite selecionar um pra
 * conversa ativa. Ao selecionar, atualiza store.context.agentId.
 */
export function AgentSelector() {
  const { data: agents, isLoading } = useAgents()
  const conv = useActiveConversation()
  const setAgent = useChatStore((s) => s.setAgent)

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
        // CARREGANDO AGENTES
      </div>
    )
  }

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        disabled={!conv}
        className={cn(
          'flex items-center gap-2 px-3 h-7 border transition-colors',
          'border-[var(--border)] hover:border-[var(--accent-cyan)]',
          open && 'border-[var(--accent-cyan)]',
          !conv && 'opacity-50 cursor-not-allowed',
        )}
      >
        <Sparkles size={12} strokeWidth={1.5} className="text-[var(--accent-cyan)]" />
        <span className="font-display uppercase tracking-[0.15em] text-[11px] text-[var(--text)]">
          {selected?.emoji ? `${selected.emoji} ` : ''}{selected?.name ?? 'AGENTE'}
        </span>
        <ChevronDown
          size={12}
          strokeWidth={1.5}
          className={cn('text-[var(--text-dim)] transition-transform', open && 'rotate-180')}
        />
      </button>

      {open && agents && (
        <div className="absolute bottom-9 right-0 z-50 w-[360px] bg-[var(--bg-base)] border border-[var(--accent-cyan)] shadow-[0_0_24px_rgba(0,212,255,0.15)]">
          {/* corner brackets */}
          <span aria-hidden className="pointer-events-none absolute -top-px -left-px h-3 w-3 border-t border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -top-px -right-px h-3 w-3 border-t border-r border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -left-px h-3 w-3 border-b border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -right-px h-3 w-3 border-b border-r border-[var(--accent-cyan)]" />

          <div className="px-3 py-2 border-b border-[var(--border-dim)] font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.2em]">
            // SELECIONE UM AGENTE ESPECIALISTA
          </div>

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
                      'w-full text-left px-3 py-3 flex items-start gap-3 transition-colors',
                      'hover:bg-[var(--bg-elevated)]',
                      isSelected && 'bg-[var(--bg-elevated)]',
                    )}
                  >
                    <span className="text-[18px] mt-[2px] leading-none w-6 text-center">
                      {a.emoji ?? '✦'}
                    </span>
                    <div className="flex-1 min-w-0">
                      <div className="font-display uppercase tracking-[0.12em] text-[12px] text-[var(--text)] flex items-center gap-2">
                        {a.name}
                        {isSelected && (
                          <span className="text-[var(--accent-cyan)]">●</span>
                        )}
                      </div>
                      <div className="font-sans text-[11px] text-[var(--text-dim)] mt-1 leading-snug">
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
