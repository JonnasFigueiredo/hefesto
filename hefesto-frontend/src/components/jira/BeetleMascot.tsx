interface Props {
  /** Tamanho em px. Default 80 (canto). */
  size?: number
  className?: string
}

/**
 * Mascote da página Jira — besouro mecânico esquemático com painel JQL
 * pulsando no abdômen. Originalmente desenhado no placeholder da Etapa 1;
 * preservado como assinatura visual da página.
 */
export function BeetleMascot({ size = 80, className }: Props) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 200 200"
      xmlns="http://www.w3.org/2000/svg"
      className={`stroke-[var(--text-muted)] fill-transparent ${className ?? ''}`}
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-label="Esquemático de um Besouro"
      role="img"
    >
      {/* Antenas */}
      <path d="M 80 30 Q 60 10 30 20" />
      <path d="M 120 30 Q 140 10 170 20" />

      {/* Mandíbulas */}
      <path d="M 85 45 C 75 25 55 35 65 55" />
      <path d="M 115 45 C 125 25 145 35 135 55" />

      {/* Cabeça */}
      <polygon points="85,45 115,45 125,60 75,60" />

      {/* Olhos */}
      <circle cx="75" cy="55" r="4" className="fill-[var(--accent-cyan)] stroke-[var(--accent-cyan)] opacity-80" />
      <circle cx="125" cy="55" r="4" className="fill-[var(--accent-cyan)] stroke-[var(--accent-cyan)] opacity-80" />

      {/* Tórax */}
      <polygon points="70,65 130,65 140,95 60,95" />
      <line x1="85" y1="65" x2="85" y2="95" />
      <line x1="115" y1="65" x2="115" y2="95" />
      <circle cx="100" cy="80" r="8" />
      <circle cx="100" cy="80" r="2" className="fill-[var(--text-muted)]" />

      {/* Carapaça / Abdômen */}
      <path d="M 55 100 L 145 100 C 155 145 130 185 100 195 C 70 185 45 145 55 100 Z" />

      {/* Divisões das asas */}
      <line x1="100" y1="100" x2="100" y2="125" />
      <line x1="100" y1="170" x2="100" y2="195" />
      <line x1="60" y1="115" x2="140" y2="115" />
      <line x1="70" y1="175" x2="130" y2="175" />

      {/* Patas Esquerdas */}
      <polyline points="70,75 40,65 20,85" />
      <polyline points="65,90 30,95 15,125" />
      <polyline points="65,115 35,130 25,160" />

      {/* Patas Direitas */}
      <polyline points="130,75 160,65 180,85" />
      <polyline points="135,90 170,95 185,125" />
      <polyline points="135,115 165,130 175,160" />

      {/* Painel Central com JQL READY */}
      <rect x="65" y="130" width="70" height="35" rx="4" strokeDasharray="3 3" className="fill-transparent" />
      <text x="100" y="146" textAnchor="middle" className="font-mono text-[11px] fill-[var(--accent-cyan)] stroke-none tracking-widest font-bold">JQL</text>
      <text x="100" y="158" textAnchor="middle" className="font-mono text-[9px] fill-[var(--text-muted)] stroke-none tracking-[0.2em]">READY</text>
    </svg>
  )
}
