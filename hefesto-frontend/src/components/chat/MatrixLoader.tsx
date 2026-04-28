import { useEffect, useState } from 'react'

/**
 * Matrix-style decoder pra usar enquanto o stream do LLM não começou.
 * Caracteres katakana + hex pulsando em verde, com um "raio" de luz
 * percorrendo a barra. Inteiramente client-side, sem deps externas.
 */

const MATRIX_CHARS =
  'ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜ' +
  '0123456789ABCDEF░▒▓█'
const NUM_CELLS = 28
const TICK_MS = 80

export function MatrixLoader() {
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), TICK_MS)
    return () => clearInterval(id)
  }, [])

  return (
    <div className="font-mono select-none" aria-label="decrypting transmission">
      {/* Header */}
      <div className="text-[10px] tracking-[0.3em] mb-2 flex items-center gap-2 text-[var(--accent-green)]">
        <span
          className="inline-block w-[6px] h-[6px] rounded-full bg-[var(--accent-green)] animate-pulse-dot"
          style={{ boxShadow: '0 0 8px rgba(0,255,159,0.8)' }}
        />
        <span style={{ textShadow: '0 0 6px rgba(0,255,159,0.5)' }}>
          DECIFRANDO TRANSMISSÃO
        </span>
        <span className="ml-auto text-[var(--text-muted)]">
          {String((tick * 3) % 9999).padStart(4, '0')}
        </span>
      </div>

      {/* Bar with cells */}
      <div
        className="flex gap-[1px] px-2 py-2 border border-[rgba(0,255,159,0.35)] bg-[rgba(0,16,8,0.6)] relative overflow-hidden"
        style={{ boxShadow: 'inset 0 0 16px rgba(0,255,159,0.08)' }}
      >
        {/* Sweep highlight */}
        <div
          aria-hidden
          className="absolute top-0 bottom-0 w-[40px] pointer-events-none"
          style={{
            left: `${((tick * 4) % (100 + 40)) - 40}%`,
            background:
              'linear-gradient(90deg, transparent, rgba(0,255,159,0.18), transparent)',
            transition: 'left 80ms linear',
          }}
        />

        {Array.from({ length: NUM_CELLS }).map((_, i) => {
          // Caractere muda em fases distintas por célula.
          const charIdx = Math.abs((tick * 3 + i * 11) % MATRIX_CHARS.length)
          const char = MATRIX_CHARS[charIdx]
          // Onda de intensidade caminhando pela barra.
          const wave = Math.sin(tick / 6 + i * 0.45)
          const isHot = wave > 0.7
          const opacity = 0.25 + Math.max(0, wave) * 0.75
          return (
            <span
              key={i}
              className="text-[14px] leading-none w-[14px] text-center inline-block transition-colors"
              style={{
                color: isHot ? '#00ff9f' : `rgba(0, 255, 159, ${opacity})`,
                textShadow: isHot
                  ? '0 0 8px #00ff9f, 0 0 4px #00ff9f, 0 0 1px #ffffff'
                  : opacity > 0.6
                    ? '0 0 4px rgba(0,255,159,0.5)'
                    : 'none',
              }}
            >
              {char}
            </span>
          )
        })}
      </div>

      {/* Footer status row */}
      <div className="flex items-center gap-3 mt-2 text-[9px] tracking-[0.25em] text-[var(--text-muted)]">
        <span>// CANAL ABERTO</span>
        <span className="flex-1 border-t border-dashed border-[var(--border-dim)]" />
        <span>{NUM_CELLS} BLOCOS</span>
      </div>
    </div>
  )
}
