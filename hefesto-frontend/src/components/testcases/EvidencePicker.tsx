import {
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
} from 'react'
import { Camera, Paperclip, Upload } from 'lucide-react'
import { useTranslation } from '@/i18n/I18nProvider'
import { useUploadEvidence } from '@/hooks/useTestCases'
import { cn } from '@/lib/cn'

interface Props {
  testCaseId: string
}

/**
 * Botão + drop zone localizado por test case. Faz upload de evidência
 * (imagem, log, qualquer arquivo) e invalida o cache de evidências do
 * caso pra renderizar imediatamente.
 */
export function EvidencePicker({ testCaseId }: Props) {
  const { t } = useTranslation()
  const upload = useUploadEvidence()
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleFiles = async (files: FileList | File[]) => {
    setError(null)
    for (const file of Array.from(files)) {
      try {
        await upload.mutateAsync({ testCaseId, file })
      } catch (e) {
        setError(e instanceof Error ? e.message : t('attach.errorUpload'))
      }
    }
  }

  const onInput = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      void handleFiles(e.target.files)
      e.target.value = ''
    }
  }

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    e.stopPropagation()
    setDragging(false)
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      void handleFiles(e.dataTransfer.files)
    }
  }

  return (
    <div
      onDragOver={(e) => {
        e.preventDefault()
        e.stopPropagation()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      className={cn(
        'inline-block',
        dragging && 'ring-1 ring-[var(--accent-cyan)]',
      )}
    >
      <input
        ref={inputRef}
        type="file"
        onChange={onInput}
        accept="image/*,text/*,application/json,application/pdf,.log,.txt,.md"
        className="hidden"
      />
      <button
        type="button"
        onClick={() => inputRef.current?.click()}
        disabled={upload.isPending}
        className={cn(
          'inline-flex items-center gap-2 px-2 h-6 border transition-colors',
          'border-[var(--border-dim)] text-[var(--text-dim)]',
          'hover:border-[var(--accent-cyan)] hover:text-[var(--accent-cyan)]',
          'font-display uppercase tracking-[0.15em] text-[10px]',
          'disabled:opacity-40 disabled:cursor-not-allowed',
        )}
        title={t('evidence.tooltip')}
      >
        {upload.isPending ? (
          <>
            <Upload size={11} strokeWidth={1.5} className="animate-pulse" />
            {t('evidence.uploading')}
          </>
        ) : dragging ? (
          <>
            <Camera size={11} strokeWidth={1.5} />
            {t('evidence.dropHere')}
          </>
        ) : (
          <>
            <Paperclip size={11} strokeWidth={1.5} />
            {t('evidence.attach')}
          </>
        )}
      </button>
      {error && (
        <div className="mt-1 font-mono text-[10px] text-[var(--accent-magenta)]">
          // {error}
        </div>
      )}
    </div>
  )
}
