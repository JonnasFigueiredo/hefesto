import { Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Frame } from '@/components/ui/Frame'
import { StatusDot } from '@/components/ui/StatusDot'
import { MessageList } from './MessageList'
import { Composer } from './Composer'
import { AdapterSelector } from './AdapterSelector'
import { ContextBar } from './ContextBar'
import { useChatStore, useActiveConversation } from '@/store/chatStore'
import { useChatStream } from '@/hooks/useChatStream'

export function ChatWindow() {
  const conv = useActiveConversation()
  const removeConversation = useChatStore((s) => s.removeConversation)
  const streamingMessageId = useChatStore((s) => s.streamingMessageId)

  const { wsState, isStreaming, error, send, abort, clearError } = useChatStream()

  const onSend = (text: string) => {
    clearError()
    send(text)
  }

  const wsLabel = wsState === 'open' ? 'CONECTADO' : wsState === 'connecting' ? 'CONECTANDO' : 'OFFLINE'
  const wsStatus = wsState === 'open' ? 'online' : wsState === 'connecting' ? 'pending' : 'offline'

  return (
    <Frame
      title={conv ? `// SESSÃO ${conv.id.slice(0, 8).toUpperCase()}` : '// SESSÃO'}
      className="flex-1 flex flex-col"
      padded={false}
    >
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-[var(--border-dim)]">
        <div className="flex-1 min-w-0">
          {conv ? (
            <div className="font-sans text-[13px] text-[var(--text)] truncate">
              {conv.title ?? null}
            </div>
          ) : (
            <div className="font-mono text-[11px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
              // NENHUMA SESSÃO SELECIONADA
            </div>
          )}
        </div>

        <div
          className="flex items-center gap-2 font-mono text-[10px] uppercase tracking-[0.15em]"
          aria-label="websocket status"
        >
          <StatusDot status={wsStatus as never} />
          <span className="text-[var(--text-muted)]">{wsLabel}</span>
        </div>

        <AdapterSelector />

        {conv && (
          <Button
            variant="ghost"
            size="sm"
            icon={<Trash2 size={12} strokeWidth={1.5} />}
            onClick={() => removeConversation(conv.id)}
            aria-label="limpar sessão"
            disabled={isStreaming}
          >
            LIMPAR
          </Button>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 min-h-0">
        <MessageList
          messages={conv?.messages ?? []}
          streamingMessageId={streamingMessageId}
          isWaitingFirstChunk={isStreaming}
        />
      </div>

      {/* Error banner */}
      {error && (
        <div className="mx-4 mb-3 border border-[var(--accent-magenta)] px-3 py-2 font-mono text-[11px] text-[var(--accent-magenta)] flex items-center gap-3">
          <span className="flex-1">// ERRO :: {error}</span>
          <button
            onClick={clearError}
            className="text-[var(--accent-magenta)] hover:text-[var(--text)] uppercase tracking-[0.18em] text-[10px]"
          >
            FECHAR
          </button>
        </div>
      )}

      {/* Context bar — agent + attachments + jira issue (logo acima do composer) */}
      {conv && <ContextBar />}

      {/* Composer */}
      <div className="px-4 pb-4 pt-3">
        <Composer
          onSend={onSend}
          onAbort={abort}
          isStreaming={isStreaming}
          disabled={wsState !== 'open'}
        />
      </div>
    </Frame>
  )
}
