import { useEffect, useRef } from 'react'
import { MessageBubble } from './MessageBubble'
import { ThinkingIndicator } from './ThinkingIndicator'
import type { Message } from '@/types/chat'

interface MessageListProps {
  messages: Message[]
  /** Id da mensagem atualmente sendo streamed (assistant em construção). */
  streamingMessageId?: string | null
  /** True quando ainda não chegou nenhum chunk (placeholder ainda vazio). */
  isWaitingFirstChunk?: boolean
}

export function MessageList({
  messages,
  streamingMessageId = null,
  isWaitingFirstChunk = false,
}: MessageListProps) {
  const endRef = useRef<HTMLDivElement>(null)

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
   READY TO CHAT`}
        </pre>
        <div className="font-mono text-[12px] text-[var(--text-dim)]">
          // TYPE A MESSAGE TO BEGIN TRANSMISSION
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-5 py-4 px-2">
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
