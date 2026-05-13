import { useTranslation } from '@/i18n/I18nProvider'
import type { Locale } from '@/i18n/translations'
import { cn } from '@/lib/cn'

/**
 * Toggle de idioma com bandeiras SVG inline. Posicionado na TopBar.
 * Estado ativo é destacado com border cyan + glow leve.
 */
export function LanguageToggle() {
  const { locale, setLocale, t } = useTranslation()

  return (
    <div
      role="group"
      aria-label={t('lang.label')}
      className="inline-flex items-center gap-1 border border-[var(--border-dim)] p-[2px]"
    >
      <FlagButton
        active={locale === 'pt-BR'}
        onClick={() => setLocale('pt-BR')}
        title={t('lang.brazilianPortuguese')}
        flag={<BrFlag />}
        code="BR"
      />
      <FlagButton
        active={locale === 'en-US'}
        onClick={() => setLocale('en-US')}
        title={t('lang.usEnglish')}
        flag={<UsFlag />}
        code="US"
      />
    </div>
  )
}

interface FlagButtonProps {
  active: boolean
  onClick: () => void
  title: string
  flag: React.ReactNode
  code: string
}

function FlagButton({ active, onClick, title, flag, code }: FlagButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      title={title}
      className={cn(
        'inline-flex items-center gap-1.5 px-2 h-6 transition-all',
        'font-mono text-[10px] uppercase tracking-[0.15em]',
        active
          ? 'bg-[var(--bg-elevated)] text-[var(--accent-cyan)] shadow-[inset_0_0_0_1px_var(--accent-cyan)]'
          : 'text-[var(--text-muted)] hover:text-[var(--text)] opacity-70 hover:opacity-100',
      )}
    >
      <span className="leading-none flex items-center">{flag}</span>
      <span>{code}</span>
    </button>
  )
}

// ---------------------------------------------------------------------------
// SVG flags (inline) — compactas, alta legibilidade em ~22×16
// ---------------------------------------------------------------------------

function BrFlag({ size = 22 }: { size?: number }) {
  const h = Math.round((size * 14) / 20)
  return (
    <svg
      viewBox="0 0 14 10"
      width={size}
      height={h}
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden
    >
      <rect width="14" height="10" fill="#009b3a" />
      <polygon points="7,1.2 12.8,5 7,8.8 1.2,5" fill="#fedf00" />
      <circle cx="7" cy="5" r="1.9" fill="#002776" />
      <path
        d="M5.3,4.6 C 5.9,4.3 8.1,4.3 8.7,4.6"
        stroke="#ffffff"
        strokeWidth="0.18"
        fill="none"
      />
    </svg>
  )
}

function UsFlag({ size = 22 }: { size?: number }) {
  const h = Math.round((size * 14) / 20)
  // 13 listras alternadas, canton azul. Aproximação simplificada.
  const stripes = []
  for (let i = 0; i < 13; i++) {
    stripes.push(
      <rect
        key={i}
        x="0"
        y={(i * 10) / 13}
        width="19"
        height={10 / 13}
        fill={i % 2 === 0 ? '#b22234' : '#ffffff'}
      />,
    )
  }
  return (
    <svg
      viewBox="0 0 19 10"
      width={size}
      height={h}
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden
    >
      {stripes}
      <rect x="0" y="0" width="7.6" height={(10 * 7) / 13} fill="#3c3b6e" />
      {/* Pontinhos brancos representando estrelas, simplificado */}
      {Array.from({ length: 4 }).map((_, row) =>
        Array.from({ length: 5 }).map((_, col) => (
          <circle
            key={`${row}-${col}`}
            cx={0.7 + col * 1.5}
            cy={0.6 + row * 1.2}
            r="0.22"
            fill="#ffffff"
          />
        )),
      )}
    </svg>
  )
}
