import { useState } from 'react'
import { Check, ChevronDown, Circle, Octagon, Slash, X } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/cn'
import { useUpdateTestCaseStatus } from '@/hooks/useTestCases'
import type { TestCase, TestCaseStatus } from '@/types/testCase'
import { EvidencePicker } from './EvidencePicker'
import { EvidenceList } from './EvidenceList'

interface Props {
  testCase: TestCase
}

const STATUS_CONFIG: Record<
  TestCaseStatus,
  { label: string; color: string; icon: typeof Circle }
> = {
  PENDING: { label: 'PENDENTE', color: 'var(--text-dim)', icon: Circle },
  PASSED: { label: 'PASSOU', color: 'var(--accent-green)', icon: Check },
  FAILED: { label: 'FALHOU', color: 'var(--accent-magenta)', icon: X },
  BLOCKED: { label: 'BLOQUEADO', color: 'var(--accent-amber)', icon: Octagon },
}

const CATEGORY_VARIANT: Record<string, 'green' | 'magenta' | 'amber' | 'dim'> = {
  POSITIVO: 'green',
  NEGATIVO: 'magenta',
  EDGE: 'amber',
}

export function TestCaseCard({ testCase: tc }: Props) {
  const [expanded, setExpanded] = useState(false)
  const updateStatus = useUpdateTestCaseStatus()

  const statusCfg = STATUS_CONFIG[tc.status]
  const StatusIcon = statusCfg.icon
  const catVariant = (tc.category && CATEGORY_VARIANT[tc.category]) ?? 'dim'

  const setStatus = (s: TestCaseStatus) => {
    updateStatus.mutate({ id: tc.id, status: s, notes: tc.notes })
  }

  return (
    <div
      className={cn(
        'relative border transition-colors',
        tc.status === 'PASSED' && 'border-[var(--accent-green)]',
        tc.status === 'FAILED' && 'border-[var(--accent-magenta)]',
        tc.status === 'BLOCKED' && 'border-[var(--accent-amber)]',
        tc.status === 'PENDING' && 'border-[var(--border-dim)]',
      )}
    >
      {/* Header (clicável pra expandir) */}
      <button
        onClick={() => setExpanded((v) => !v)}
        className="w-full text-left flex items-start gap-3 p-3"
      >
        <StatusIcon
          size={14}
          strokeWidth={2}
          className="mt-0.5 shrink-0"
          style={{ color: statusCfg.color }}
        />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            {tc.code && (
              <span className="font-mono text-[11px] text-[var(--accent-cyan)] tracking-[0.05em] font-bold">
                {tc.code}
              </span>
            )}
            {tc.category && (
              <Badge variant={catVariant}>{tc.category}</Badge>
            )}
            {tc.priority && (
              <span className="font-mono text-[9px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
                {tc.priority}
              </span>
            )}
          </div>
          <div className="font-sans text-[13px] text-[var(--text)]">
            {tc.title}
          </div>
        </div>
        <ChevronDown
          size={14}
          strokeWidth={1.5}
          className={cn(
            'text-[var(--text-muted)] transition-transform shrink-0 mt-1',
            expanded && 'rotate-180',
          )}
        />
      </button>

      {/* Body expansível */}
      {expanded && (
        <div className="px-3 pb-3 space-y-3 border-t border-[var(--border-dim)] pt-3">
          {tc.preconditions && (
            <Section label="// PRÉ-CONDIÇÕES">
              <div className="text-[12px] text-[var(--text-dim)]">{tc.preconditions}</div>
            </Section>
          )}

          {tc.steps.length > 0 && (
            <Section label={`// PASSOS (${tc.steps.length})`}>
              <ol className="list-decimal list-inside space-y-1 text-[12px] text-[var(--text)]">
                {tc.steps.map((s, i) => (
                  <li key={i}>{s}</li>
                ))}
              </ol>
            </Section>
          )}

          {tc.expectedResult && (
            <Section label="// RESULTADO ESPERADO">
              <div className="text-[12px] text-[var(--text-dim)]">{tc.expectedResult}</div>
            </Section>
          )}

          {/* Status actions */}
          <Section label="// EXECUÇÃO">
            <div className="flex flex-wrap gap-2">
              {(['PENDING', 'PASSED', 'FAILED', 'BLOCKED'] as TestCaseStatus[]).map(
                (s) => {
                  const cfg = STATUS_CONFIG[s]
                  const SIcon = cfg.icon
                  const active = tc.status === s
                  return (
                    <button
                      key={s}
                      onClick={() => setStatus(s)}
                      disabled={updateStatus.isPending}
                      className={cn(
                        'inline-flex items-center gap-1.5 px-2 h-6 border transition-colors',
                        'font-display uppercase tracking-[0.15em] text-[10px]',
                        active
                          ? 'border-current'
                          : 'border-[var(--border-dim)] hover:border-current',
                        updateStatus.isPending && 'opacity-40 cursor-wait',
                      )}
                      style={{ color: cfg.color }}
                    >
                      <SIcon size={10} strokeWidth={2} />
                      {cfg.label}
                    </button>
                  )
                },
              )}
              <span className="ml-auto" />
              <EvidencePicker testCaseId={tc.id} />
            </div>
            {updateStatus.isError && (
              <div className="mt-2 font-mono text-[10px] text-[var(--accent-magenta)] border border-[var(--accent-magenta)] px-2 py-1">
                // ERRO :: {updateStatus.error instanceof Error ? updateStatus.error.message : 'falha na requisição'}
              </div>
            )}
          </Section>

          {/* Evidências */}
          <Section label="// EVIDÊNCIAS">
            <EvidenceList testCaseId={tc.id} />
            {/* fallback quando sem evidências */}
            <FallbackEvidence testCaseId={tc.id} />
          </Section>
        </div>
      )}
    </div>
  )
}

function Section({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.2em] mb-1.5">
        {label}
      </div>
      <div>{children}</div>
    </div>
  )
}

// Hint quando não há evidências ainda — usa o mesmo hook dentro
import { useEvidence } from '@/hooks/useTestCases'

function FallbackEvidence({ testCaseId }: { testCaseId: string }) {
  const { data } = useEvidence(testCaseId)
  if (data && data.length > 0) return null
  return (
    <div className="font-mono text-[10px] text-[var(--text-muted)] flex items-center gap-2">
      <Slash size={10} strokeWidth={1.5} />
      NENHUMA EVIDÊNCIA — ANEXE PRINTS, LOGS OU RESPOSTAS DE API NO BOTÃO ACIMA.
    </div>
  )
}
