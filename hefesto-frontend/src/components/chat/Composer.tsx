import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { Send, Square } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { KBD } from '@/components/ui/KBD'

interface ComposerProps {
  onSend: (text: string) => void
  onAbort?: () => void
  disabled?: boolean
  isSending?: boolean
  /** True quando há streaming em curso (mostra STOP em vez de TRANSMIT). */
  isStreaming?: boolean
}

export function Composer({
  onSend,
  onAbort,
  disabled = false,
  isSending = false,
  isStreaming = false,
}: ComposerProps) {
  const [value, setValue] = useState('')
  const taRef = useRef<HTMLTextAreaElement>(null)

  // Auto-resize do textarea (até 200px).
  useEffect(() => {
    const ta = taRef.current
    if (!ta) return
    ta.style.height = 'auto'
    ta.style.height = `${Math.min(ta.scrollHeight, 200)}px`
  }, [value])

  const submit = () => {
    const trimmed = value.trim()
    if (!trimmed || disabled || isSending) return
    onSend(trimmed)
    setValue('')
  }

  const handleKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    const isModSubmit = (e.metaKey || e.ctrlKey) && e.key === 'Enter'
    if (isModSubmit) {
      e.preventDefault()
      submit()
    }
  }

  return (
    <div className="relative border border-[var(--border)] focus-within:border-[var(--accent-cyan)] focus-within:shadow-[0_0_0_1px_var(--accent-cyan),0_0_12px_rgba(0,212,255,0.2)] transition-all">
      {/* corner brackets */}
      <span aria-hidden className="pointer-events-none absolute -top-px -left-px h-3 w-3 border-t border-l border-[var(--accent-cyan)]" />
      <span aria-hidden className="pointer-events-none absolute -top-px -right-px h-3 w-3 border-t border-r border-[var(--accent-cyan)]" />
      <span aria-hidden className="pointer-events-none absolute -bottom-px -left-px h-3 w-3 border-b border-l border-[var(--accent-cyan)]" />
      <span aria-hidden className="pointer-events-none absolute -bottom-px -right-px h-3 w-3 border-b border-r border-[var(--accent-cyan)]" />

      <textarea
        ref={taRef}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={handleKey}
        disabled={disabled || isSending || isStreaming}
        rows={2}
        placeholder={
          isStreaming
            ? '// STREAMING EM ANDAMENTO — PARAR PARA INTERROMPER'
            : isSending
              ? '// AGUARDANDO RESPOSTA...'
              : '// DIGITE SUA MENSAGEM...'
        }
        className="w-full px-4 py-3 bg-transparent text-[13px] text-[var(--text)] placeholder:text-[var(--text-muted)] focus:outline-none resize-none font-sans disabled:opacity-60"
      />

      <div className="flex items-center justify-between px-4 py-2 border-t border-[var(--border-dim)] font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
        <div>
          {isStreaming
            ? '// AO VIVO'
            : value.length > 0
              ? `${value.length} caracteres`
              : '// PRONTO'}
        </div>
        <div className="flex items-center gap-3">
          {!isStreaming && (
            <span className="hidden sm:flex items-center gap-1">
              <KBD>⌘</KBD>
              <span>+</span>
              <KBD>⏎</KBD>
            </span>
          )}
          {isStreaming ? (
            <Button
              variant="danger"
              size="sm"
              onClick={onAbort}
              icon={<Square size={12} strokeWidth={1.5} fill="currentColor" />}
            >
              PARAR
            </Button>
          ) : (
            <Button
              variant="primary"
              size="sm"
              disabled={disabled || isSending || value.trim().length === 0}
              onClick={submit}
              icon={<Send size={12} strokeWidth={1.5} />}
            >
              {isSending ? 'ENVIANDO' : 'ENVIAR'}
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
