import { useTranslation } from '@/i18n/I18nProvider'

/**
 * Indicador de "processando" no estilo equalizer — 4 barras pulsando em fases
 * diferentes. Mostrado enquanto o adapter ainda não respondeu.
 */
export function ThinkingIndicator() {
  const { t } = useTranslation()
  return (
    <div className="flex items-center gap-2 py-2">
      <div className="flex items-end gap-[3px] h-4">
        {[0, 0.15, 0.3, 0.45].map((delay, i) => (
          <span
            key={i}
            className="w-[3px] h-full bg-[var(--accent-cyan)] eq-bar origin-bottom"
            style={{ animationDelay: `${delay}s` }}
          />
        ))}
      </div>
      <span className="font-mono text-[10px] text-[var(--text-dim)] tracking-[0.2em] uppercase">
        {t('thinking.processing')}
      </span>
    </div>
  )
}
