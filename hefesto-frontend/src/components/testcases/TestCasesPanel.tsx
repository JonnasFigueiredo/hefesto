import { TestTube } from 'lucide-react'
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
      <div className="flex items-center gap-2 px-3 py-2 border-b border-[var(--border-dim)] bg-[var(--bg-elevated)]">
        <TestTube size={12} strokeWidth={1.5} className="text-[var(--accent-cyan)]" />
        <span className="font-display uppercase tracking-[0.18em] text-[11px] text-[var(--accent-cyan)] glow-cyan">
          CASOS DE TESTE EXTRAÍDOS
        </span>
        <span className="ml-auto flex items-center gap-3 font-mono text-[10px] text-[var(--text-muted)]">
          <span>
            <span className="text-[var(--text)]">{cases.length}</span> total
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
      </div>

      <div className="p-3 space-y-2">
        {cases.map((c) => (
          <TestCaseCard key={c.id} testCase={c} />
        ))}
      </div>
    </div>
  )
}
