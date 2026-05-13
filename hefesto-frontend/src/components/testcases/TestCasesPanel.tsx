import { FileDown, TestTube } from 'lucide-react'
import { useTranslation } from '@/i18n/I18nProvider'
import { useTestCasesByMessage } from '@/hooks/useTestCases'
import { TestCaseCard } from './TestCaseCard'

interface Props {
  messageId: string
}

/**
 * Painel exibido logo abaixo de uma mensagem do assistant quando o
 * backend extraiu casos de teste daquela resposta. Polling automático
 * pra capturar casos que aparecem após o streaming terminar.
 */
export function TestCasesPanel({ messageId }: Props) {
  const { t } = useTranslation()
  const { data: cases, isLoading } = useTestCasesByMessage(messageId)

  if (isLoading) return null
  if (!cases || cases.length === 0) return null

  // Agrupa por status pra mostrar resumo
  const counts = {
    PENDING: 0,
    PASSED: 0,
    FAILED: 0,
    BLOCKED: 0,
  } as Record<string, number>
  for (const c of cases) counts[c.status] = (counts[c.status] ?? 0) + 1

  return (
    <div className="mt-4 border border-[var(--accent-cyan)] bg-[rgba(0,212,255,0.02)]">
      {/* corner brackets */}
      <span aria-hidden className="pointer-events-none absolute h-2 w-2" />

      {/* Header */}
      <div className="flex items-center gap-2 px-3 py-2 border-b border-[var(--border-dim)] bg-[var(--bg-elevated)] flex-wrap">
        <TestTube size={12} strokeWidth={1.5} className="text-[var(--accent-cyan)]" />
        <span className="font-display uppercase tracking-[0.18em] text-[11px] text-[var(--accent-cyan)] glow-cyan">
          {t('tc.panelTitle')}
        </span>
        <span className="flex items-center gap-3 font-mono text-[10px] text-[var(--text-muted)] ml-auto mr-3">
          <span>
            <span className="text-[var(--text)]">{cases.length}</span> {t('tc.totalSuffix')}
          </span>
          {counts.PASSED > 0 && (
            <span className="text-[var(--accent-green)]">{counts.PASSED} ✓</span>
          )}
          {counts.FAILED > 0 && (
            <span className="text-[var(--accent-magenta)]">{counts.FAILED} ✗</span>
          )}
          {counts.BLOCKED > 0 && (
            <span className="text-[var(--accent-amber)]">{counts.BLOCKED} ⊘</span>
          )}
          {counts.PENDING > 0 && (
            <span className="text-[var(--text-dim)]">{counts.PENDING} ○</span>
          )}
        </span>
        <button
          onClick={() => {
            const url = `/api/reports/test-cases-by-message?messageId=${encodeURIComponent(messageId)}`
            window.open(url, '_blank', 'noopener,noreferrer')
          }}
          title={t('tc.generateReportTooltip')}
          className="inline-flex items-center gap-1.5 h-6 px-2 border border-[var(--accent-cyan)] text-[var(--accent-cyan)] bg-[rgba(0,212,255,0.05)] hover:bg-[rgba(0,212,255,0.12)] hover:shadow-[0_0_8px_rgba(0,212,255,0.4)] transition-all font-display uppercase tracking-[0.15em] text-[10px]"
        >
          <FileDown size={11} strokeWidth={1.5} />
          {t('tc.generateReport')}
        </button>
      </div>

      <div className="p-3 space-y-2">
        {cases.map((c) => (
          <TestCaseCard key={c.id} testCase={c} />
        ))}
      </div>
    </div>
  )
}
