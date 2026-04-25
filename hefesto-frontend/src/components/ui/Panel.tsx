import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

interface PanelProps {
  children: ReactNode
  className?: string
}

/**
 * Elevated solid block, no brackets. Used inside Frames or as standalone.
 */
export function Panel({ children, className }: PanelProps) {
  return (
    <div className={cn('bg-[var(--bg-elevated)] p-4', className)}>{children}</div>
  )
}
