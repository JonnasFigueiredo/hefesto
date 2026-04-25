import type { TextareaHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  mono?: boolean
}

export function Textarea({ mono = false, className, ...rest }: TextareaProps) {
  return (
    <textarea
      {...rest}
      className={cn(
        'w-full px-3 py-2 bg-[var(--bg-overlay)] border border-[var(--border)] text-[var(--text)]',
        'placeholder:text-[var(--text-muted)]',
        'focus:outline-none focus:border-[var(--accent-cyan)] focus:shadow-[0_0_0_1px_var(--accent-cyan),0_0_12px_rgba(0,212,255,0.2)]',
        'transition-all resize-none',
        mono ? 'font-mono text-[12px]' : 'font-sans text-[13px]',
        className,
      )}
    />
  )
}
