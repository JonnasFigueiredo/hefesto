import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Textarea } from '@/components/ui/Textarea'
import {
  useCreateIssue,
  useDraftStory,
  useDraftStoryFromImage,
  useJiraIssueTypes,
  useJiraProjects,
} from '@/hooks/useJira'
import type { StoryDraft } from '@/types/jira'
import { useAdapters } from '@/hooks/useAdapters'
import type { CreatedIssue } from '@/types/jira'
import { cn } from '@/lib/cn'
import { useTranslation } from '@/i18n/I18nProvider'

interface CreateIssueModalProps {
  open: boolean
  /** Pré-seleciona o projeto (ex: derivado da última issue aberta). */
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

  const projects = useJiraProjects(open)
  const [projectKey, setProjectKey] = useState('')
  const issueTypes = useJiraIssueTypes(projectKey || null)
  const [issueType, setIssueType] = useState('')

  const [summary, setSummary] = useState('')
  const [description, setDescription] = useState('')
  const [criteria, setCriteria] = useState('')

  // --- assistência de IA ---
  const adapters = useAdapters()
  const draft = useDraftStory()
  const draftImage = useDraftStoryFromImage()
  const [aiModel, setAiModel] = useState('')
  const [aiContext, setAiContext] = useState('')
  const [aiImage, setAiImage] = useState<File | null>(null)

  // Default do modelo: primeiro adapter disponível.
  useEffect(() => {
    if (aiModel || !adapters.data?.length) return
    const firstAvailable = adapters.data.find((a) => a.available)
    if (firstAvailable) setAiModel(firstAvailable.id)
  }, [adapters.data, aiModel])

  const aiPending = draft.isPending || draftImage.isPending
  const aiError = draft.error || draftImage.error

  const applyDraft = (d: StoryDraft) => {
    if (d.summary) setSummary(d.summary)
    if (d.description) setDescription(d.description)
    if (d.acceptanceCriteria?.length) setCriteria(d.acceptanceCriteria.join('\n'))
  }

  const handleGenerate = () => {
    if (aiPending) return
    // Com imagem (design de tela): usa o fluxo de visão (Claude). Sem imagem:
    // exige contexto em texto e usa o modelo selecionado.
    if (aiImage) {
      draftImage.mutate(
        { image: aiImage, context: aiContext.trim() || undefined },
        { onSuccess: applyDraft },
      )
    } else {
      if (!aiModel || aiContext.trim().length === 0) return
      draft.mutate({ model: aiModel, context: aiContext.trim() }, { onSuccess: applyDraft })
    }
  }

  // Seleciona um projeto padrão assim que a lista chega: o sugerido (se válido)
  // ou o primeiro. Evita projeto inexistente como o erro que motivou os dropdowns.
  useEffect(() => {
    if (projectKey || !projects.data?.length) return
    const fromDefault = projects.data.find((p) => p.key === defaultProjectKey)
    setProjectKey(fromDefault?.key ?? projects.data[0].key)
  }, [projects.data, defaultProjectKey, projectKey])

  // Sempre que os tipos do projeto chegam, garante um tipo válido selecionado.
  useEffect(() => {
    if (!issueTypes.data?.length) return
    if (!issueTypes.data.includes(issueType)) {
      setIssueType(issueTypes.data[0])
    }
  }, [issueTypes.data, issueType])

  if (!open) return null

  const canSubmit =
    projectKey.trim().length > 0 &&
    issueType.trim().length > 0 &&
    summary.trim().length > 0 &&
    !create.isPending

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!canSubmit) return
    const acceptanceCriteria = criteria
      .split('\n')
      .map((l) => l.trim())
      .filter((l) => l.length > 0)
    create.mutate(
      {
        projectKey,
        summary: summary.trim(),
        description: description.trim() || undefined,
        acceptanceCriteria: acceptanceCriteria.length ? acceptanceCriteria : undefined,
        issueType,
      },
      {
        onSuccess: (created) => {
          onCreated(created)
          setSummary('')
          setDescription('')
          setCriteria('')
        },
      },
    )
  }

  const selectCls = cn(
    'w-full h-9 px-3 bg-[var(--bg-overlay)] border border-[var(--border)] text-[var(--text)]',
    'focus:outline-none focus:border-[var(--accent-cyan)] focus:shadow-[0_0_0_1px_var(--accent-cyan),0_0_12px_rgba(0,212,255,0.2)]',
    'font-mono text-[12px] transition-all',
  )

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
          {/* Assistência de IA: gera rascunho a partir de requisitos/contexto */}
          <div className="border border-[var(--accent-cyan)]/40 bg-[rgba(0,212,255,0.04)] p-3 space-y-2">
            <div className="flex items-center justify-between gap-2">
              <span className="font-display uppercase tracking-[0.15em] text-[10px] text-[var(--accent-cyan)]">
                {t('jira.aiTitle')}
              </span>
              <select
                className={cn(selectCls, 'h-7 w-auto text-[11px]')}
                value={aiModel}
                onChange={(e) => setAiModel(e.target.value)}
              >
                {adapters.data?.map((a) => (
                  <option key={a.id} value={a.id} disabled={!a.available}>
                    {a.displayName}
                    {a.available ? '' : ' (indisponível)'}
                  </option>
                ))}
              </select>
            </div>
            <Textarea
              rows={3}
              value={aiContext}
              onChange={(e) => setAiContext(e.target.value)}
              placeholder={t('jira.aiContextPlaceholder')}
            />

            {/* Anexar design de tela (imagem) — usa modelo com visão (Claude) */}
            <div className="flex items-center gap-2 flex-wrap">
              <label className="inline-flex items-center gap-2 h-7 px-3 cursor-pointer border border-[var(--border)] text-[var(--text-dim)] hover:border-[var(--accent-cyan)] hover:text-[var(--accent-cyan)] font-display uppercase tracking-[0.12em] text-[10px] transition-colors">
                🖼️ {t('jira.aiImageAttach')}
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => setAiImage(e.target.files?.[0] ?? null)}
                />
              </label>
              {aiImage && (
                <span className="font-mono text-[10px] text-[var(--accent-cyan)] flex items-center gap-2">
                  {aiImage.name}
                  <button
                    type="button"
                    onClick={() => setAiImage(null)}
                    className="text-[var(--text-dim)] hover:text-[var(--accent-magenta)]"
                    aria-label={t('jira.aiImageRemove')}
                  >
                    ✕
                  </button>
                </span>
              )}
            </div>
            {aiImage && (
              <div className="font-mono text-[10px] text-[var(--text-muted)]">
                {t('jira.aiImageNote')}
              </div>
            )}

            <div className="flex items-center justify-between gap-2">
              <span className="font-mono text-[10px] text-[var(--text-muted)]">
                {t('jira.aiHint')}
              </span>
              <Button
                type="button"
                variant="primary"
                size="sm"
                onClick={handleGenerate}
                disabled={aiPending || (!aiImage && (!aiModel || aiContext.trim().length === 0))}
              >
                {aiPending ? t('jira.aiGenerating') : t('jira.aiGenerate')}
              </Button>
            </div>
            {aiError && (
              <div className="font-mono text-[10px] text-[var(--accent-magenta)]">
                {aiError instanceof Error ? aiError.message : t('jira.aiError')}
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Field label={t('jira.fieldProject')}>
              <select
                className={selectCls}
                value={projectKey}
                onChange={(e) => {
                  setProjectKey(e.target.value)
                  setIssueType('')
                }}
                disabled={projects.isLoading}
              >
                {projects.isLoading && <option>...</option>}
                {projects.data?.map((p) => (
                  <option key={p.key} value={p.key}>
                    {p.key} — {p.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label={t('jira.fieldType')}>
              <select
                className={selectCls}
                value={issueType}
                onChange={(e) => setIssueType(e.target.value)}
                disabled={issueTypes.isLoading || !projectKey}
              >
                {issueTypes.isLoading && <option>...</option>}
                {issueTypes.data?.map((tp) => (
                  <option key={tp} value={tp}>
                    {tp}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <Field label={t('jira.fieldSummary')}>
            <Input
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              placeholder={t('jira.fieldSummaryPlaceholder')}
              autoFocus
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
