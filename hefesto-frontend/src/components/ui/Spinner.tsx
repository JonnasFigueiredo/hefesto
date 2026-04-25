import { cn } from '@/lib/cn'

interface SpinnerProps {
  size?: number
  className?: string
}

/**
 * HUD-style spinner — open arc rotating.
 */
export function Spinner({ size = 16, className }: SpinnerProps) {
  return (
    <span
      role="status"
      aria-label="loading"
      style={{ width: size, height: size }}
      className={cn('inline-block animate-spin-slow', className)}
    >
      <svg viewBox="0 0 24 24" width={size} height={size} fill="none" aria-hidden>
        <circle
          cx="12"
          cy="12"
          r="9"
          stroke="var(--accent-cyan)"
          strokeWidth="1.5"
          strokeDasharray="40 16"
          strokeLinecap="round"
        />
        <circle cx="12" cy="3" r="1" fill="var(--accent-cyan)" />
      </svg>
    </span>
  )
}
