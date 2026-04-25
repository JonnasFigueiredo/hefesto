import { useEffect, useState } from 'react'

/**
 * Returns a Date that updates every second. Used by the TopBar clock.
 */
export function useClock(): Date {
  const [now, setNow] = useState<Date>(() => new Date())
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])
  return now
}

export function formatClock(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  const date = `${d.getUTCFullYear()}.${pad(d.getUTCMonth() + 1)}.${pad(d.getUTCDate())}`
  const time = `${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}:${pad(d.getUTCSeconds())}`
  return `${date} // ${time} UTC`
}
