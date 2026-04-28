import { useEffect, useState } from 'react'

/**
 * Retorna um Date que atualiza a cada segundo. Usado pelo relógio do TopBar.
 */
export function useClock(): Date {
  const [now, setNow] = useState<Date>(() => new Date())
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])
  return now
}

/**
 * Formata Date no padrão brasileiro: dd/MM/aaaa // HH:mm:ss
 * Usa horário local da máquina do usuário.
 */
export function formatClock(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  const date = `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  return `${date} // ${time}`
}
