import { FileText, X } from 'lucide-react'
import { useAttachments, useDeleteAttachment } from '@/hooks/useAttachments'
import { useActiveConversation, useChatStore } from '@/store/chatStore'

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

/**
 * Mostra os arquivos anexados à conversa ativa como chips removíveis.
 * Cada chip tem ícone, nome (truncado), tamanho e X pra desanexar.
 *
 * Remover do chip: tira da conversa (não deleta do store global).
 * Pra deletar definitivamente, há atalho extra (shift+click).
 */
export function AttachmentsList() {
  const conv = useActiveConversation()
  const { data: allAttachments } = useAttachments()
  const removeAttachment = useChatStore((s) => s.removeAttachment)
  const deleteAtt = useDeleteAttachment()

  if (!conv || conv.context.attachmentIds.length === 0) return null

  const attached = conv.context.attachmentIds
    .map((id) => allAttachments?.find((a) => a.id === id))
    .filter((a): a is NonNullable<typeof a> => !!a)

  if (attached.length === 0) return null

  return (
    <div className="flex flex-wrap gap-2">
      {attached.map((a) => (
        <div
          key={a.id}
          className="group inline-flex items-center gap-2 px-2 py-1 border border-[var(--border-dim)] hover:border-[var(--accent-cyan)] transition-colors"
          title="Clique no X para desanexar; Shift+clique para deletar do servidor"
        >
          <FileText size={12} strokeWidth={1.5} className="text-[var(--accent-cyan)]" />
          <span className="font-mono text-[11px] text-[var(--text)] max-w-[180px] truncate">
            {a.filename}
          </span>
          <span className="font-mono text-[9px] text-[var(--text-muted)]">
            {formatBytes(a.sizeBytes)}
          </span>
          <button
            onClick={(e) => {
              if (e.shiftKey) {
                deleteAtt.mutate(a.id)
              }
              removeAttachment(conv.id, a.id)
            }}
            aria-label={`detach ${a.filename}`}
            className="text-[var(--text-muted)] hover:text-[var(--accent-magenta)] transition-colors"
          >
            <X size={12} strokeWidth={2} />
          </button>
        </div>
      ))}
    </div>
  )
}
