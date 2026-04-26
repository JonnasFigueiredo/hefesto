/**
 * Tiny fetch wrapper. Returns parsed JSON or throws.
 * Tracks last latency for the topbar indicator.
 */

let lastLatency: number | null = null
const listeners = new Set<(ms: number | null) => void>()

export function getLastLatency(): number | null {
  return lastLatency
}

export function subscribeLatency(fn: (ms: number | null) => void): () => void {
  listeners.add(fn)
  return () => listeners.delete(fn)
}

function setLatency(ms: number | null) {
  lastLatency = ms
  listeners.forEach((fn) => fn(ms))
}

export async function http<T = unknown>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const start = performance.now()
  try {
    const res = await fetch(path, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers ?? {}),
      },
    })
    setLatency(Math.round(performance.now() - start))
    if (!res.ok) {
      const body = await res.text().catch(() => '')
      throw new Error(`${res.status} ${res.statusText} :: ${body}`)
    }
    if (res.status === 204) return undefined as T
    return (await res.json()) as T
  } catch (err) {
    setLatency(null)
    throw err
  }
}
