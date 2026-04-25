import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

type BadgeVariant = 'default' | 'cyan' | 'amber' | 'magenta' | 'green' | 'dim'

interface BadgeProps {
  children: ReactNode
  variant?: BadgeVariant
  className?: string
}

const VARIANTS: Record<BadgeVariant, string> = {
  default: 'border-[var(--border)] text-[var(--text-dim)]',
  cyan: 'border-[var(--accent-cyan)] text-[var(--accent-cyan)] bg-[rgba(0,212,255,0.06)]',
  amber: 'border-[var(--accent-amber)] text-[var(--accent-amber)] bg-[rgba(255,181,71,0.06)]',
  magenta:
    'border-[var(--accent-magenta)] text-[var(--accent-magenta)] bg-[rgba(255,61,138,0.06)]',
  green: 'border-[var(--accent-green)] text-[var(--accent-green)] bg-[rgba(0,255,159,0.06)]',
  dim: 'border-[var(--border-dim)] text-[var(--text-muted)]',
}

export function Badge({ children, variant = 'default', className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center px-2 h-5 border text-[10px] font-mono uppercase tracking-[0.15em]',
        VARIANTS[variant],
        className,
      )}
    >
      {children}
    </span>
  )
}
