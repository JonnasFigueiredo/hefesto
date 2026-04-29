import { useEffect } from 'react'
import { ConversationList } from '@/components/chat/ConversationList'
import { ChatWindow } from '@/components/chat/ChatWindow'
import { useAdapters } from '@/hooks/useAdapters'
import { useConversations } from '@/hooks/useConversations'
import { useChatStore } from '@/store/chatStore'

export function ChatPage() {
  // Aciona o fetch de adapters globalmente — auto-seleciona o primeiro disponível.
  useAdapters()

  // Hidratação: busca conversas persistidas no SQLite e mescla com o store local.
  const { data: serverConversations } = useConversations()
  const loadFromServer = useChatStore((s) => s.loadFromServer)

  useEffect(() => {
    if (serverConversations && serverConversations.length > 0) {
      loadFromServer(serverConversations)
    }
  }, [serverConversations, loadFromServer])

  return (
    <div className="h-full flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          CHAT
        </h1>
      </div>

      <div className="flex-1 flex gap-4 min-h-0">
        <ConversationList />
        <ChatWindow />
      </div>
    </div>
  )
}
