import { useRef, useState, type DragEvent, type ChangeEvent } from 'react'
import { Paperclip, Upload } from 'lucide-react'
import { useUploadAttachment } from '@/hooks/useAttachments'
import { useActiveConversation, useChatStore } from '@/store/chatStore'
import { cn } from '@/lib/cn'

const ACCEPT = '.txt,.md,.markdown,.json,.yml,.yaml,.csv,.log,text/*,application/json'

/**
 * Botão + drop zone pra fazer upload de arquivos. Após upload, o id é
 * automaticamente anexado à conversa ativa via store.addAttachment.
 */
export function AttachmentPicker() {
  const upload = useUploadAttachment()
  const conv = useActiveConversation()
  const addAttachment = useChatStore((s) => s.addAttachment)

  const inputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setDragging] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleFiles = async (files: FileList | File[]) => {
    if (!conv) {
      setError('Crie uma sessão antes de anexar arquivos.')
      return
    }
    setError(null)
    for (const file of Array.from(files)) {
      try {
        const att = await upload.mutateAsync(file)
        addAttachment(conv.id, att.id)
      } catch (e) {
        setError(e instanceof Error ? e.message : 'falha ao enviar arquivo')
      }
    }
  }

  const onInput = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      void handleFiles(e.target.files)
      // reset pra permitir re-upload do mesmo arquivo
      e.target.value = ''
    }
  }

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    setDragging(false)
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      void handleFiles(e.dataTransfer.files)
    }
  }

  return (
    <div
      onDragOver={(e) => {
        e.preventDefault()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      className={cn(
        'relative inline-block',
        isDragging && 'ring-1 ring-[var(--accent-cyan)]',
      )}
    >
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPT}
        multiple
        onChange={onInput}
        className="hidden"
        aria-label="upload attachments"
      />
      <button
        type="button"
        onClick={() => inputRef.current?.click()}
        disabled={upload.isPending || !conv}
        className={cn(
          'inline-flex items-center gap-2 h-7 px-3 border transition-colors',
          'border-[var(--border)] text-[var(--text-dim)]',
          'hover:border-[var(--accent-cyan)] hover:text-[var(--accent-cyan)]',
          'disabled:opacity-40 disabled:cursor-not-allowed',
          'font-display uppercase tracking-[0.15em] text-[11px]',
        )}
        title="Anexar arquivos (.txt, .md, .json, .yml). Ou arraste e solte na tela."
      >
        {upload.isPending ? (
          <>
            <Upload size={12} strokeWidth={1.5} className="animate-pulse" />
            ENVIANDO
          </>
        ) : (
          <>
            <Paperclip size={12} strokeWidth={1.5} />
            ANEXAR
          </>
        )}
      </button>

      {isDragging && (
        <div
          aria-hidden
          className="fixed inset-0 z-40 pointer-events-none flex items-center justify-center"
          style={{
            background: 'rgba(0, 212, 255, 0.05)',
          }}
        >
          <div className="bg-[var(--bg-base)] border-2 border-dashed border-[var(--accent-cyan)] px-8 py-6 font-display uppercase tracking-[0.2em] text-[14px] text-[var(--accent-cyan)]">
            // SOLTE PARA ANEXAR
          </div>
        </div>
      )}

      {error && (
        <div className="absolute top-9 right-0 z-50 max-w-xs border border-[var(--accent-magenta)] bg-[var(--bg-base)] px-3 py-2 font-mono text-[10px] text-[var(--accent-magenta)]">
          // {error}
          <button
            onClick={() => setError(null)}
            className="ml-2 underline"
          >
            FECHAR
          </button>
        </div>
      )}
    </div>
  )
}
