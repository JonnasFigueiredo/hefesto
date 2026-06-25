import { FileText, Image as ImageIcon, X } from 'lucide-react'
import { evidenceDownloadUrl } from '@/api/testCases'
import { useTranslation } from '@/i18n/I18nProvider'
import { useDeleteEvidence, useEvidence } from '@/hooks/useTestCases'
import { cn } from '@/lib/cn'

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function isImage(contentType: string | null): boolean {
  return !!contentType && contentType.startsWith('image/')
}

interface Props {
  testCaseId: string
}

/**
 * Lista evidências anexadas a um caso de teste. Mostra thumbnail pra
 * imagens e chip com ícone pra outros tipos. Permite remover (Shift+X
 * ainda exclui mas confirma — aqui simplificamos pra só X).
 */
export function EvidenceList({ testCaseId }: Props) {
  const { t } = useTranslation()
  const { data: evidence } = useEvidence(testCaseId)
  const del = useDeleteEvidence()

  if (!evidence || evidence.length === 0) return null

  return (
    <div className="flex flex-wrap gap-2 mt-2">
      {evidence.map((e) => {
        const url = evidenceDownloadUrl(testCaseId, e.id)
        if (isImage(e.contentType)) {
          return (
            <div
              key={e.id}
              className="relative group border border-[var(--border-dim)] hover:border-[var(--accent-cyan)] transition-colors"
              style={{ width: 100, height: 100 }}
              title={`${e.filename} · ${formatBytes(e.sizeBytes)}`}
            >
              <a href={url} target="_blank" rel="noopener noreferrer" className="block w-full h-full">
                <img
                  src={url}
                  alt={e.filename}
                  className="w-full h-full object-cover"
                />
              </a>
              <button
                onClick={() =>
                  del.mutate({ testCaseId, evidenceId: e.id })
                }
                aria-label={t('evidence.removeAria')}
                className="absolute top-0 right-0 p-1 bg-[var(--bg-base)] text-[var(--text-muted)] hover:text-[var(--accent-magenta)] opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <X size={12} strokeWidth={2} />
              </button>
              <div className="absolute bottom-0 left-0 right-0 bg-[rgba(7,9,15,0.85)] px-1 py-[2px] font-mono text-[9px] text-[var(--text-dim)] truncate">
                {e.filename}
              </div>
            </div>
          )
        }
        // Não-imagem: chip
        return (
          <div
            key={e.id}
            className={cn(
              'group inline-flex items-center gap-2 px-2 py-1',
              'border border-[var(--border-dim)] hover:border-[var(--accent-cyan)] transition-colors',
            )}
            title={`${e.filename} · ${formatBytes(e.sizeBytes)}`}
          >
            <FileText size={12} strokeWidth={1.5} className="text-[var(--accent-amber)]" />
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className="font-mono text-[11px] text-[var(--text)] hover:text-[var(--accent-cyan)] max-w-[180px] truncate"
            >
              {e.filename}
            </a>
            <span className="font-mono text-[9px] text-[var(--text-muted)]">
              {formatBytes(e.sizeBytes)}
            </span>
            <button
              onClick={() => del.mutate({ testCaseId, evidenceId: e.id })}
              aria-label={t('evidence.removeAria')}
              className="text-[var(--text-muted)] hover:text-[var(--accent-magenta)]"
            >
              <X size={12} strokeWidth={2} />
            </button>
          </div>
        )
      })}
      {/* Hint pra accessibility — mostra ícone de tipo na lista */}
      <span aria-hidden className="hidden">
        <ImageIcon />
      </span>
    </div>
  )
}
