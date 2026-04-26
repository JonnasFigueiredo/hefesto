import { useEffect } from 'react'
import { Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Frame } from '@/components/ui/Frame'
import { MessageList } from './MessageList'
import { Composer } from './Composer'
import { AdapterSelector } from './AdapterSelector'
import { useChatStore, useActiveConversation } from '@/store/chatStore'
import { useSendChat } from '@/hooks/useSendChat'

export function ChatWindow() {
  const conv = useActiveConversation()
  const removeConversation = useChatStore((s) => s.removeConversation)
  const send = useSendChat()

  // Limpa estado da mutation quando troca de conversa.
  useEffect(() => {
    send.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conv?.id])

  const onSend = (text: string) => {
    send.mutate({ message: text })
  }

  return (
    <Frame title={conv ? `// SESSION ${conv.id.slice(0, 8).toUpperCase()}` : '// SESSION'} className="flex-1 flex flex-col" padded={false}>
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-[var(--border-dim)]">
        <div className="flex-1 min-w-0">
          {conv ? (
            <div className="font-sans text-[13px] text-[var(--text)] truncate">
              {conv.title ?? <span className="text-[var(--text-muted)] italic">// untitled session</span>}
            </div>
          ) : (
            <div className="font-mono text-[11px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
              // NO SESSION SELECTED
            </div>
          )}
        </div>
        <AdapterSelector />
        {conv && (
          <Button
            variant="ghost"
            size="sm"
            icon={<Trash2 size={12} strokeWidth={1.5} />}
            onClick={() => removeConversation(conv.id)}
            aria-label="clear session"
          >
            CLEAR
          </Button>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4">
        <MessageList
          messages={conv?.messages ?? []}
          isThinking={send.isPending}
        />
      </div>

      {/* Error banner */}
      {send.isError && (
        <div className="mx-4 mb-3 border border-[var(--accent-magenta)] px-3 py-2 font-mono text-[11px] text-[var(--accent-magenta)]">
          // ERROR :: {send.error instanceof Error ? send.error.message : 'unknown'}
        </div>
      )}

      {/* Composer */}
      <div className="px-4 pb-4">
        <Composer onSend={onSend} disabled={!conv && false} isSending={send.isPending} />
      </div>
    </Frame>
  )
}
