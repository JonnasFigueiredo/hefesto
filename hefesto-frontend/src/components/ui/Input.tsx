import type { InputHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  /** Use mono font (good for JQL, IDs, etc.) */
  mono?: boolean
}

export function Input({ mono = false, className, ...rest }: InputProps) {
  return (
    <input
      {...rest}
      className={cn(
        'w-full h-9 px-3 bg-[var(--bg-overlay)] border border-[var(--border)] text-[var(--text)]',
        'placeholder:text-[var(--text-muted)]',
        'focus:outline-none focus:border-[var(--accent-cyan)] focus:shadow-[0_0_0_1px_var(--accent-cyan),0_0_12px_rgba(0,212,255,0.2)]',
        'transition-all',
        mono ? 'font-mono text-[12px]' : 'font-sans text-[13px]',
        className,
      )}
    />
  )
}
