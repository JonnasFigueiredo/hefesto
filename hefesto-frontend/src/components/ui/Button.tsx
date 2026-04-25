import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { cn } from '@/lib/cn'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  icon?: ReactNode
  iconPosition?: 'left' | 'right'
}

const VARIANTS: Record<ButtonVariant, string> = {
  primary:
    'border border-[var(--accent-cyan)] text-[var(--accent-cyan)] bg-[rgba(0,212,255,0.05)] hover:bg-[rgba(0,212,255,0.12)] hover:shadow-[0_0_12px_rgba(0,212,255,0.4)]',
  secondary:
    'border border-[var(--border)] text-[var(--text)] hover:border-[var(--accent-cyan)] hover:text-[var(--accent-cyan)]',
  ghost:
    'border border-transparent text-[var(--text-dim)] hover:text-[var(--accent-cyan)] hover:border-[var(--border-dim)]',
  danger:
    'border border-[var(--accent-magenta)] text-[var(--accent-magenta)] hover:bg-[rgba(255,61,138,0.1)]',
}

const SIZES: Record<ButtonSize, string> = {
  sm: 'h-7 px-3 text-[11px]',
  md: 'h-9 px-4 text-[12px]',
}

export function Button({
  variant = 'secondary',
  size = 'md',
  icon,
  iconPosition = 'left',
  className,
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      className={cn(
        'inline-flex items-center gap-2 font-display uppercase tracking-[0.15em] transition-all',
        'focus:outline-none focus-visible:ring-1 focus-visible:ring-[var(--accent-cyan)]',
        'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:shadow-none',
        SIZES[size],
        VARIANTS[variant],
        className,
      )}
    >
      {icon && iconPosition === 'left' && <span className="flex items-center">{icon}</span>}
      <span>{children}</span>
      {icon && iconPosition === 'right' && <span className="flex items-center">{icon}</span>}
    </button>
  )
}
