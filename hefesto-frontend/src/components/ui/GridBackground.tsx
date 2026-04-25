/**
 * Fixed full-viewport SVG dot grid. Renders behind everything.
 */
export function GridBackground() {
  return (
    <div
      aria-hidden
      className="pointer-events-none fixed inset-0 -z-10"
      style={{
        backgroundImage:
          'radial-gradient(circle, rgba(122, 138, 168, 0.18) 1px, transparent 1px)',
        backgroundSize: '24px 24px',
        backgroundPosition: '0 0',
        maskImage:
          'radial-gradient(ellipse at center, black 30%, transparent 80%)',
        WebkitMaskImage:
          'radial-gradient(ellipse at center, black 30%, transparent 80%)',
      }}
    />
  )
}
