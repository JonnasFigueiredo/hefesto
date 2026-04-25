interface ScanlineOverlayProps {
  enabled?: boolean
}

/**
 * Animated horizontal scanline that drifts down the screen.
 * Off by default; toggle from settings.
 */
export function ScanlineOverlay({ enabled = false }: ScanlineOverlayProps) {
  if (!enabled) return null
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 z-[60] overflow-hidden">
      <div
        className="absolute left-0 right-0 h-[2px] animate-scanline"
        style={{
          background:
            'linear-gradient(to bottom, transparent, rgba(0,212,255,0.35), transparent)',
        }}
      />
    </div>
  )
}
