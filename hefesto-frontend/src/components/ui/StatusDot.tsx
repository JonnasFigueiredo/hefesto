import { cn } from '@/lib/cn'

type StatusVariant = 'online' | 'offline' | 'pending' | 'warning' | 'unknown'

interface StatusDotProps {
  status: StatusVariant
  /** Size in px. Default 6. */
  size?: number
  className?: string
}

const VARIANT_COLOR: Record<StatusVariant, string> = {
  online: 'bg-[var(--accent-green)] shadow-[0_0_8px_rgba(0,255,159,0.6)]',
  offline: 'bg-[var(--accent-magenta)] shadow-[0_0_8px_rgba(255,61,138,0.6)]',
  pending: 'bg-[var(--accent-amber)] shadow-[0_0_8px_rgba(255,181,71,0.6)]',
  warning: 'bg-[var(--accent-amber)] shadow-[0_0_8px_rgba(255,181,71,0.6)]',
  unknown: 'bg-[var(--text-muted)]',
}

export function StatusDot({ status, size = 6, className }: StatusDotProps) {
  const animate = status === 'online' || status === 'pending'
  return (
    <span
      role="status"
      aria-label={status}
      style={{ width: size, height: size }}
      className={cn(
        'inline-block rounded-full shrink-0',
        VARIANT_COLOR[status],
        animate && 'animate-pulse-dot',
        className,
      )}
    />
  )
}
