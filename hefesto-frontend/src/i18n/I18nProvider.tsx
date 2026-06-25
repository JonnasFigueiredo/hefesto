import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  DEFAULT_LOCALE,
  interpolate,
  LOCALES,
  translations,
  type Dict,
  type Locale,
} from './translations'

interface I18nContextValue {
  locale: Locale
  setLocale: (l: Locale) => void
  t: (key: string, vars?: Record<string, string | number>, fallback?: string) => string
}

const STORAGE_KEY = 'hefesto.locale'

const I18nContext = createContext<I18nContextValue | null>(null)

function detectInitialLocale(): Locale {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved && (LOCALES as string[]).includes(saved)) {
      return saved as Locale
    }
  } catch {
    /* localStorage indisponível (SSR, modo privado, etc.) */
  }
  // Auto-detect: pt-BR se navigator.language começar com "pt", senão default
  if (typeof navigator !== 'undefined') {
    const nav = navigator.language?.toLowerCase() ?? ''
    if (nav.startsWith('en')) return 'en-US'
  }
  return DEFAULT_LOCALE
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(() => detectInitialLocale())

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, locale)
    } catch {
      /* ignore */
    }
    if (typeof document !== 'undefined') {
      document.documentElement.lang = locale
    }
  }, [locale])

  const setLocale = useCallback((l: Locale) => setLocaleState(l), [])

  const dict: Dict = translations[locale] ?? translations[DEFAULT_LOCALE]

  const t = useCallback(
    (key: string, vars?: Record<string, string | number>, fallback?: string) => {
      const raw = dict[key]
      if (raw === undefined) {
        // Fallback: tenta no idioma default antes de usar a key como label
        const defaultRaw = translations[DEFAULT_LOCALE][key]
        if (defaultRaw !== undefined) return interpolate(defaultRaw, vars)
        return fallback ?? key
      }
      return interpolate(raw, vars)
    },
    [dict],
  )

  const value = useMemo<I18nContextValue>(
    () => ({ locale, setLocale, t }),
    [locale, setLocale, t],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useTranslation(): I18nContextValue {
  const ctx = useContext(I18nContext)
  if (!ctx) {
    throw new Error('useTranslation precisa estar dentro de <I18nProvider>')
  }
  return ctx
}
