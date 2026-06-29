import { useEffect, useRef } from 'react'
import { MessageBubble } from './MessageBubble'
import { ThinkingIndicator } from './ThinkingIndicator'
import type { Message } from '@/types/chat'
import { useTranslation } from '@/i18n/I18nProvider'

interface MessageListProps {
  messages: Message[]
  streamingMessageId?: string | null
  isWaitingFirstChunk?: boolean
}

export function MessageList({
  messages,
  streamingMessageId = null,
  isWaitingFirstChunk = false,
}: MessageListProps) {
  const endRef = useRef<HTMLDivElement>(null)
  const { t } = useTranslation()

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages.length, messages[messages.length - 1]?.content?.length])

  if (messages.length === 0 && !isWaitingFirstChunk) {
    return (
      <div className="h-full flex flex-col items-center justify-center gap-4 text-center py-16">
        <pre className="font-mono text-[11px] text-[var(--text-muted)] leading-tight">
{`     /\\___/\\
    (  o o  )
    /   |   \\
   (___|___)__
   ${t('empty.readyToChat')}`}
        </pre>
        <div className="font-mono text-[12px] text-[var(--text-dim)]">
          {t('empty.typeToBegin')}
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-5 py-4 px-2 min-w-0">
      {messages.map((m) => (
        <MessageBubble
          key={m.id}
          message={m}
          streaming={m.id === streamingMessageId}
        />
      ))}
      {isWaitingFirstChunk && !streamingMessageId && (
        <div className="flex justify-start">
          <ThinkingIndicator />
        </div>
      )}
      <div ref={endRef} />
    </div>
  )
}
