import { useState, type FormEvent } from 'react'
import { Search } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { useTranslation } from '@/i18n/I18nProvider'

interface Props {
  initialJql?: string
  onExecute: (jql: string) => void
  isLoading?: boolean
}

interface Suggestion {
  labelKey: string
  jql: string
}

const SUGGESTIONS: Suggestion[] = [
  { labelKey: 'jql.shortcutMine', jql: 'assignee = currentUser() ORDER BY updated DESC' },
  { labelKey: 'jql.shortcutInProgress', jql: 'status = "In Progress" ORDER BY updated DESC' },
  { labelKey: 'jql.shortcutOpen', jql: 'status = Open ORDER BY created DESC' },
  { labelKey: 'jql.shortcutLast7d', jql: 'updated >= -7d ORDER BY updated DESC' },
]

export function JqlSearchBar({ initialJql = '', onExecute, isLoading }: Props) {
  const [jql, setJql] = useState(initialJql)
  const { t } = useTranslation()

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (jql.trim().length === 0) return
    onExecute(jql.trim())
  }

  return (
    <div className="space-y-3">
      <form onSubmit={submit} className="flex items-stretch gap-2">
        <div className="flex-1 relative">
          <Search
            size={14}
            strokeWidth={1.5}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] pointer-events-none"
          />
          <Input
            mono
            value={jql}
            onChange={(e) => setJql(e.target.value)}
            placeholder={t('jql.placeholder')}
            className="pl-9"
          />
        </div>
        <Button
          type="submit"
          variant="primary"
          size="md"
          disabled={isLoading || jql.trim().length === 0}
        >
          {isLoading ? t('jql.executing') : t('jql.execute')}
        </Button>
      </form>

      <div className="flex flex-wrap gap-2">
        <span className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em] py-1">
          {t('jql.shortcuts')}
        </span>
        {SUGGESTIONS.map((s) => (
          <button
            key={s.labelKey}
            type="button"
            onClick={() => {
              setJql(s.jql)
              onExecute(s.jql)
            }}
            className="px-2 py-1 border border-[var(--border-dim)] hover:border-[var(--accent-cyan)] hover:text-[var(--accent-cyan)] font-mono text-[10px] uppercase tracking-[0.15em] text-[var(--text-dim)] transition-colors"
          >
            {t(s.labelKey)}
          </button>
        ))}
      </div>
    </div>
  )
}
