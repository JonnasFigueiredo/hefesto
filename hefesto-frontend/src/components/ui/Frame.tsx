import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

type FrameVariant = 'default' | 'accent' | 'dim' | 'danger'

interface FrameProps {
  children: ReactNode
  title?: string
  variant?: FrameVariant
  className?: string
  /** Inner padding control. Default: p-4 */
  padded?: boolean
}

const VARIANT_BORDER: Record<FrameVariant, string> = {
  default: 'border-[var(--border)]',
  accent: 'border-[var(--accent-cyan)]',
  dim: 'border-[var(--border-dim)]',
  danger: 'border-[var(--accent-magenta)]',
}

const VARIANT_BRACKET: Record<FrameVariant, string> = {
  default: 'border-[var(--accent-cyan)]',
  accent: 'border-[var(--accent-cyan)]',
  dim: 'border-[var(--text-muted)]',
  danger: 'border-[var(--accent-magenta)]',
}

const VARIANT_TITLE: Record<FrameVariant, string> = {
  default: 'text-[var(--accent-cyan)]',
  accent: 'text-[var(--accent-cyan)]',
  dim: 'text-[var(--text-muted)]',
  danger: 'text-[var(--accent-magenta)]',
}

/**
 * Container with 1px border and corner brackets. Optional title chip on top edge.
 */
export function Frame({
  children,
  title,
  variant = 'default',
  className,
  padded = true,
}: FrameProps) {
  return (
    <div className={cn('relative border', VARIANT_BORDER[variant], className)}>
      {/* Corner brackets */}
      <span
        aria-hidden
        className={cn(
          'pointer-events-none absolute -top-px -left-px h-3 w-3 border-t border-l',
          VARIANT_BRACKET[variant],
        )}
      />
      <span
        aria-hidden
        className={cn(
          'pointer-events-none absolute -top-px -right-px h-3 w-3 border-t border-r',
          VARIANT_BRACKET[variant],
        )}
      />
      <span
        aria-hidden
        className={cn(
          'pointer-events-none absolute -bottom-px -left-px h-3 w-3 border-b border-l',
          VARIANT_BRACKET[variant],
        )}
      />
      <span
        aria-hidden
        className={cn(
          'pointer-events-none absolute -bottom-px -right-px h-3 w-3 border-b border-r',
          VARIANT_BRACKET[variant],
        )}
      />

      {title && (
        <div
          className={cn(
            'absolute -top-2 left-3 px-2 bg-[var(--bg-base)] font-mono text-[10px] uppercase tracking-[0.2em]',
            VARIANT_TITLE[variant],
          )}
        >
          {title}
        </div>
      )}

      {padded ? <div className="p-4">{children}</div> : children}
    </div>
  )
}
