import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

interface KBDProps {
  children: ReactNode
  className?: string
}

export function KBD({ children, className }: KBDProps) {
  return (
    <kbd
      className={cn(
        'inline-flex items-center justify-center min-w-[20px] h-5 px-1.5',
        'bg-[var(--bg-overlay)] border border-[var(--border-dim)]',
        'font-mono text-[10px] text-[var(--text-dim)]',
        className,
      )}
    >
      {children}
    </kbd>
  )
}
