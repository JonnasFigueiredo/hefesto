import { useState } from 'react'
import { Check, ChevronDown, Circle, Octagon, Slash, X } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/cn'
import { useTranslation } from '@/i18n/I18nProvider'
import { useUpdateTestCaseStatus, useEvidence } from '@/hooks/useTestCases'
import type { TestCase, TestCaseStatus } from '@/types/testCase'
import { EvidencePicker } from './EvidencePicker'
import { EvidenceList } from './EvidenceList'

interface Props {
  testCase: TestCase
}

const STATUS_ICONS: Record<TestCaseStatus, typeof Circle> = {
  PENDING: Circle,
  PASSED: Check,
  FAILED: X,
  BLOCKED: Octagon,
}

const STATUS_COLORS: Record<TestCaseStatus, string> = {
  PENDING: 'var(--text-dim)',
  PASSED: 'var(--accent-green)',
  FAILED: 'var(--accent-magenta)',
  BLOCKED: 'var(--accent-amber)',
}

const CATEGORY_VARIANT: Record<string, 'green' | 'magenta' | 'amber' | 'dim'> = {
  POSITIVO: 'green',
  NEGATIVO: 'magenta',
  EDGE: 'amber',
}

export function TestCaseCard({ testCase: tc }: Props) {
  const { t } = useTranslation()
  const [expanded, setExpanded] = useState(false)
  const updateStatus = useUpdateTestCaseStatus()

  const StatusIcon = STATUS_ICONS[tc.status]
  const statusColor = STATUS_COLORS[tc.status]
  const catVariant = (tc.category && CATEGORY_VARIANT[tc.category]) ?? 'dim'

  const STATUS_LABELS: Record<TestCaseStatus, string> = {
    PENDING: t('tc.statusPending'),
    PASSED: t('tc.statusPassed'),
    FAILED: t('tc.statusFailed'),
    BLOCKED: t('tc.statusBlocked'),
  }

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
          style={{ color: statusColor }}
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
            <Section label={t('tc.sectionPreconditions')}>
              <div className="text-[12px] text-[var(--text-dim)]">{tc.preconditions}</div>
            </Section>
          )}

          {tc.steps.length > 0 && (
            <Section label={t('tc.sectionSteps', { count: tc.steps.length })}>
              <ol className="list-decimal list-inside space-y-1 text-[12px] text-[var(--text)]">
                {tc.steps.map((s, i) => (
                  <li key={i}>{s}</li>
                ))}
              </ol>
            </Section>
          )}

          {tc.expectedResult && (
            <Section label={t('tc.sectionExpected')}>
              <div className="text-[12px] text-[var(--text-dim)]">{tc.expectedResult}</div>
            </Section>
          )}

          {/* Status actions */}
          <Section label={t('tc.sectionExecution')}>
            <div className="flex flex-wrap gap-2">
              {(['PENDING', 'PASSED', 'FAILED', 'BLOCKED'] as TestCaseStatus[]).map((s) => {
                const SIcon = STATUS_ICONS[s]
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
                    style={{ color: STATUS_COLORS[s] }}
                  >
                    <SIcon size={10} strokeWidth={2} />
                    {STATUS_LABELS[s]}
                  </button>
                )
              })}
              <span className="ml-auto" />
              <EvidencePicker testCaseId={tc.id} />
            </div>
            {updateStatus.isError && (
              <div className="mt-2 font-mono text-[10px] text-[var(--accent-magenta)] border border-[var(--accent-magenta)] px-2 py-1">
                {t('tc.errorPrefix')}{' '}
                {updateStatus.error instanceof Error
                  ? updateStatus.error.message
                  : t('tc.errorFallback')}
              </div>
            )}
          </Section>

          {/* Evidências */}
          <Section label={t('tc.sectionEvidence')}>
            <EvidenceList testCaseId={tc.id} />
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

function FallbackEvidence({ testCaseId }: { testCaseId: string }) {
  const { t } = useTranslation()
  const { data } = useEvidence(testCaseId)
  if (data && data.length > 0) return null
  return (
    <div className="font-mono text-[10px] text-[var(--text-muted)] flex items-center gap-2">
      <Slash size={10} strokeWidth={1.5} />
      {t('tc.noEvidence')}
    </div>
  )
}
