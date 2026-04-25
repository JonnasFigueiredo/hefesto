import { Frame } from '@/components/ui/Frame'
import { Badge } from '@/components/ui/Badge'

export function JiraPage() {
  return (
    <div className="h-full flex flex-col gap-6">
      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          JIRA
        </h1>
        <Badge variant="dim">STAGE 1 // STUB</Badge>
      </div>

      <Frame title="// ISSUE TRACKER" className="flex-1">
        <div className="h-full flex flex-col items-center justify-center text-center gap-4 py-16">
          
          {/* Arte do Besouro em SVG */}
          <div className="relative flex justify-center items-center py-4">
            <svg
              width="200"
              height="200"
              viewBox="0 0 200 200"
              xmlns="http://www.w3.org/2000/svg"
              className="stroke-[var(--text-muted)] fill-transparent"
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
              
              {/* Olhos (com a cor de destaque) */}
              <circle cx="75" cy="55" r="4" className="fill-[var(--accent-cyan)] stroke-[var(--accent-cyan)] opacity-80" />
              <circle cx="125" cy="55" r="4" className="fill-[var(--accent-cyan)] stroke-[var(--accent-cyan)] opacity-80" />

              {/* Tórax (Mecânico/Engrenagem) */}
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
          </div>

          <div className="font-mono text-[12px] text-[var(--text-dim)] mt-2">
            // JIRA INTEGRATION COMING IN STAGE 4
          </div>
        </div>
      </Frame>
    </div>
  )
}