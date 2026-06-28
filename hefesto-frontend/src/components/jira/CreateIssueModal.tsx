import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import { useCreateIssue } from '@/hooks/useJira'
import type { CreatedIssue } from '@/types/jira'
import { useTranslation } from '@/i18n/I18nProvider'

interface CreateIssueModalProps {
  open: boolean
  /** Pré-preenche o projeto (ex: derivado da última issue aberta). */
  defaultProjectKey?: string
  onClose: () => void
  onCreated: (issue: CreatedIssue) => void
}

export function CreateIssueModal({
  open,
  defaultProjectKey = '',
  onClose,
  onCreated,
}: CreateIssueModalProps) {
  const { t } = useTranslation()
  const create = useCreateIssue()

  const [projectKey, setProjectKey] = useState(defaultProjectKey)
  const [summary, setSummary] = useState('')
  const [description, setDescription] = useState('')
  const [criteria, setCriteria] = useState('')
  const [issueType, setIssueType] = useState('Story')

  if (!open) return null

  const canSubmit =
    projectKey.trim().length > 0 && summary.trim().length > 0 && !create.isPending

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!canSubmit) return
    const acceptanceCriteria = criteria
      .split('\n')
      .map((l) => l.trim())
      .filter((l) => l.length > 0)
    create.mutate(
      {
        projectKey: projectKey.trim(),
        summary: summary.trim(),
        description: description.trim() || undefined,
        acceptanceCriteria: acceptanceCriteria.length ? acceptanceCriteria : undefined,
        issueType: issueType.trim() || undefined,
      },
      {
        onSuccess: (created) => {
          onCreated(created)
          // Reset pra próxima criação.
          setSummary('')
          setDescription('')
          setCriteria('')
        },
      },
    )
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      onMouseDown={onClose}
    >
      <div
        className="w-full max-w-xl bg-[var(--bg-panel)] border border-[var(--accent-cyan)] shadow-[0_0_30px_rgba(0,212,255,0.15)]"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-3 border-b border-[var(--border-dim)]">
          <h2 className="font-display uppercase tracking-[0.2em] text-[14px] text-[var(--accent-cyan)] glow-cyan">
            {t('jira.createTitle')}
          </h2>
          <button
            onClick={onClose}
            className="font-mono text-[16px] text-[var(--text-dim)] hover:text-[var(--accent-magenta)]"
            aria-label={t('jira.createCancel')}
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div className="grid grid-cols-[1fr_140px] gap-3">
            <Field label={t('jira.fieldProject')}>
              <Input
                mono
                value={projectKey}
                onChange={(e) => setProjectKey(e.target.value.toUpperCase())}
                placeholder="PROJ"
                autoFocus
              />
            </Field>
            <Field label={t('jira.fieldType')}>
              <Input mono value={issueType} onChange={(e) => setIssueType(e.target.value)} />
            </Field>
          </div>

          <Field label={t('jira.fieldSummary')}>
            <Input
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              placeholder={t('jira.fieldSummaryPlaceholder')}
            />
          </Field>

          <Field label={t('jira.fieldDescription')}>
            <Textarea
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('jira.fieldDescriptionPlaceholder')}
            />
          </Field>

          <Field label={t('jira.fieldCriteria')} hint={t('jira.fieldCriteriaHint')}>
            <Textarea
              rows={3}
              mono
              value={criteria}
              onChange={(e) => setCriteria(e.target.value)}
              placeholder={'Dado ...\nQuando ...\nEntão ...'}
            />
          </Field>

          {create.isError && (
            <div className="border border-[var(--accent-magenta)] px-3 py-2 font-mono text-[11px] text-[var(--accent-magenta)]">
              {create.error instanceof Error ? create.error.message : t('jira.createError')}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-1">
            <Button type="button" variant="ghost" onClick={onClose}>
              {t('jira.createCancel')}
            </Button>
            <Button type="submit" variant="primary" disabled={!canSubmit}>
              {create.isPending ? t('jira.createSubmitting') : t('jira.createSubmit')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <label className="block">
      <span className="block font-display uppercase tracking-[0.15em] text-[10px] text-[var(--text-dim)] mb-1">
        {label}
        {hint ? <span className="ml-2 text-[var(--text-muted)] normal-case tracking-normal">{hint}</span> : null}
      </span>
      {children}
    </label>
  )
}
