import { useEffect, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { StatusDot } from '@/components/ui/StatusDot'
import { useChatStore } from '@/store/chatStore'
import { useAdapters } from '@/hooks/useAdapters'
import { cn } from '@/lib/cn'

export function AdapterSelector() {
  const { data: adapters, isLoading, isError } = useAdapters()
  const selectedId = useChatStore((s) => s.selectedAdapterId)
  const setSelected = useChatStore((s) => s.setSelectedAdapter)

  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const selected = adapters?.find((a) => a.id === selectedId)

  if (isLoading) {
    return (
      <div className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em] px-3 py-1 border border-[var(--border-dim)]">
        // CARREGANDO ADAPTERS
      </div>
    )
  }

  if (isError || !adapters || adapters.length === 0) {
    return (
      <div className="font-mono text-[10px] text-[var(--accent-magenta)] uppercase tracking-[0.18em] px-3 py-1 border border-[var(--accent-magenta)]">
        // ADAPTERS INDISPONÍVEIS
      </div>
    )
  }

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className={cn(
          'flex items-center gap-2 px-3 h-7 border border-[var(--border)] hover:border-[var(--accent-cyan)] transition-colors',
          open && 'border-[var(--accent-cyan)]',
        )}
      >
        <StatusDot status={selected?.available ? 'online' : 'offline'} />
        <span className="font-display uppercase tracking-[0.15em] text-[11px] text-[var(--text)]">
          {selected ? selected.id : '--'}
        </span>
        <ChevronDown
          size={12}
          strokeWidth={1.5}
          className={cn('text-[var(--text-dim)] transition-transform', open && 'rotate-180')}
        />
      </button>

      {open && (
        <div className="absolute top-9 right-0 z-50 w-[280px] bg-[var(--bg-base)] border border-[var(--accent-cyan)] shadow-[0_0_24px_rgba(0,212,255,0.15)]">
          {/* corner brackets */}
          <span aria-hidden className="pointer-events-none absolute -top-px -left-px h-3 w-3 border-t border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -top-px -right-px h-3 w-3 border-t border-r border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -left-px h-3 w-3 border-b border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -right-px h-3 w-3 border-b border-r border-[var(--accent-cyan)]" />

          <div className="px-3 py-2 border-b border-[var(--border-dim)] font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.2em]">
            // SELECIONE O ADAPTER LLM
          </div>

          <ul>
            {adapters.map((a) => {
              const isSelected = a.id === selectedId
              const disabled = !a.available
              return (
                <li key={a.id}>
                  <button
                    disabled={disabled}
                    onClick={() => {
                      setSelected(a.id)
                      setOpen(false)
                    }}
                    className={cn(
                      'w-full text-left px-3 py-3 flex items-start gap-3 transition-colors',
                      'hover:bg-[var(--bg-elevated)]',
                      isSelected && 'bg-[var(--bg-elevated)]',
                      disabled && 'opacity-40 cursor-not-allowed hover:bg-transparent',
                    )}
                  >
                    <StatusDot
                      status={a.available ? 'online' : 'offline'}
                      className="mt-1.5"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="font-display uppercase tracking-[0.12em] text-[12px] text-[var(--text)]">
                        {a.displayName}
                      </div>
                      <div className="font-mono text-[10px] text-[var(--text-muted)] mt-0.5">
                        {a.id}
                        {disabled && ' // INDISPONÍVEL'}
                      </div>
                    </div>
                    {isSelected && (
                      <span className="text-[var(--accent-cyan)] font-mono text-[10px]">●</span>
                    )}
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
