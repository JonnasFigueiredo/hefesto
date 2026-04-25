import { Frame } from '@/components/ui/Frame'
import { Badge } from '@/components/ui/Badge'

export function ChatPage() {
  return (
    <div className="h-full flex flex-col gap-6">
      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          CHAT
        </h1>
        <Badge variant="dim">STAGE 1 // STUB</Badge>
      </div>

      <Frame title="// SESSION" className="flex-1">
        <div className="h-full flex flex-col items-center justify-center text-center gap-4 py-16">
          <pre className="font-mono text-[11px] text-[var(--text-muted)] leading-tight">
{`    ___________________
   |                   |
   |   AWAITING INIT   |
   |___________________|
        |        |
        |________|`}
          </pre>
          <div className="font-mono text-[12px] text-[var(--text-dim)]">
            // CHAT MODULE NOT YET IMPLEMENTED
          </div>
          <div className="font-mono text-[10px] text-[var(--text-muted)] max-w-md">
            STAGE 2 WILL ENABLE THE LLM ADAPTER PIPE.
            <br />
            STAGE 3 WILL ENABLE WEBSOCKET STREAMING.
          </div>
        </div>
      </Frame>
    </div>
  )
}
