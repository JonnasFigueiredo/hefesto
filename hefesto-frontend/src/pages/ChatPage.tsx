import { Badge } from '@/components/ui/Badge'
import { ConversationList } from '@/components/chat/ConversationList'
import { ChatWindow } from '@/components/chat/ChatWindow'
import { useAdapters } from '@/hooks/useAdapters'

export function ChatPage() {
  // Aciona o fetch de adapters globalmente — auto-seleciona o primeiro disponível.
  useAdapters()

  return (
    <div className="h-full flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          CHAT
        </h1>
        <Badge variant="cyan">STAGE 2 // SYNC</Badge>
      </div>

      <div className="flex-1 flex gap-4 min-h-0">
        <ConversationList />
        <ChatWindow />
      </div>
    </div>
  )
}
