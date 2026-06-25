import { Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/cn'
import { useChatStore } from '@/store/chatStore'
import { deleteConversation } from '@/api/conversations'
import { useTranslation } from '@/i18n/I18nProvider'

export function ConversationList() {
  const conversations = useChatStore((s) => s.conversations)
  const activeId = useChatStore((s) => s.activeId)
  const setActive = useChatStore((s) => s.setActive)
  const removeConversation = useChatStore((s) => s.removeConversation)
  const selectedAdapterId = useChatStore((s) => s.selectedAdapterId)
  const createConversation = useChatStore((s) => s.createConversation)
  const { t } = useTranslation()

  const handleNew = () => {
    if (!selectedAdapterId) return
    createConversation(selectedAdapterId)
  }

  return (
    <aside className="w-[280px] shrink-0 h-full flex flex-col border border-[var(--border-dim)] bg-[var(--bg-base)]/50">
      <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--border-dim)]">
        <h2 className="font-display uppercase tracking-[0.2em] text-[12px] text-[var(--text-dim)]">
          {t('chat.sessions')}
        </h2>
        <Button
          variant="primary"
          size="sm"
          icon={<Plus size={12} strokeWidth={1.5} />}
          onClick={handleNew}
          disabled={!selectedAdapterId}
        >
          {t('chat.newSession')}
        </Button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {conversations.length === 0 ? (
          <div className="p-6 text-center font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
            {t('chat.noActiveSessions')}
          </div>
        ) : (
          <ul>
            {conversations.map((c) => {
              const isActive = c.id === activeId
              return (
                <li
                  key={c.id}
                  className={cn(
                    'group relative border-b border-[var(--border-dim)]',
                  )}
                >
                  <button
                    onClick={() => setActive(c.id)}
                    className={cn(
                      'w-full text-left px-4 py-3 transition-colors',
                      'hover:bg-[var(--bg-elevated)]',
                      isActive && 'bg-[var(--bg-elevated)]',
                    )}
                  >
                    {/* Active marker */}
                    {isActive && (
                      <span
                        aria-hidden
                        className="absolute left-0 top-0 bottom-0 w-[2px] bg-[var(--accent-cyan)]"
                      />
                    )}

                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-mono text-[10px] text-[var(--accent-cyan)] tracking-[0.1em]">
                        #{c.id.slice(0, 8)}
                      </span>
                    </div>

                    <div className="font-sans text-[12px] text-[var(--text)] line-clamp-2 mb-2">
                      {c.title ?? <span className="text-[var(--text-muted)] italic">{t('chat.untitled')}</span>}
                    </div>

                    <Badge variant={isActive ? 'cyan' : 'dim'}>{c.adapterId}</Badge>
                  </button>

                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      // Backend delete só pra ids reais (não local-*).
                      if (!c.id.startsWith('local-')) {
                        void deleteConversation(c.id).catch(() => {
                          // segue mesmo se falhar — usuário verá ainda no banco depois.
                        })
                      }
                      removeConversation(c.id)
                    }}
                    aria-label={t('chat.deleteAria')}
                    className="absolute top-3 right-3 p-1 text-[var(--text-muted)] hover:text-[var(--accent-magenta)] opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <Trash2 size={12} strokeWidth={1.5} />
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </aside>
  )
}
