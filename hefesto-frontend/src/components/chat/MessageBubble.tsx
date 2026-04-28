import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { cn } from '@/lib/cn'
import type { Message } from '@/types/chat'
import { MatrixLoader } from './MatrixLoader'

interface MessageBubbleProps {
  message: Message
  /** Mostra cursor piscando indicando que ainda está chegando texto. */
  streaming?: boolean
}

function formatTime(ts: number): string {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function MessageBubble({ message, streaming = false }: MessageBubbleProps) {
  const time = formatTime(message.timestamp)

  if (message.role === 'user') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[70%]">
          <div className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em] mb-1 text-right">
            VOCÊ // {time}
          </div>
          <div className="bg-[var(--bg-overlay)] border-r-2 border-r-[var(--accent-cyan)] border-t border-b border-l border-[var(--border-dim)] px-4 py-3 text-[13px] text-[var(--text)] whitespace-pre-wrap leading-relaxed">
            {message.content}
          </div>
        </div>
      </div>
    )
  }

  if (message.role === 'system' || message.role === 'jira_context') {
    return (
      <div className="w-full">
        <div className="relative border border-[var(--border-dim)]">
          <div className="absolute -top-2 left-3 px-2 bg-[var(--bg-base)] font-mono text-[10px] uppercase tracking-[0.2em] text-[var(--text-muted)]">
            {message.role === 'jira_context' ? '// CONTEXTO JIRA INJETADO' : '// SISTEMA'}
          </div>
          <pre className="px-4 py-4 font-mono text-[11px] text-[var(--text-dim)] whitespace-pre-wrap break-words leading-relaxed">
            {message.content}
          </pre>
        </div>
      </div>
    )
  }

  // assistant
  const meta = message.meta
  return (
    <div className="flex justify-start">
      <div className="max-w-[85%] w-full">
        <div className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em] mb-1 flex items-center gap-3">
          <span className="text-[var(--accent-cyan)]">
            {meta?.adapterId?.toUpperCase() ?? 'ASSISTANT'}
          </span>
          <span>// {time}</span>
          {meta?.latencyMs !== undefined && (
            <span className="text-[var(--text-muted)]">{meta.latencyMs} ms</span>
          )}
          {meta?.model && (
            <span className="text-[var(--text-muted)]">{meta.model}</span>
          )}
          {streaming && (
            <span className="text-[var(--accent-cyan)] flex items-center gap-1">
              <span className="w-[6px] h-[6px] rounded-full bg-[var(--accent-cyan)] animate-pulse-dot" />
              STREAMING
            </span>
          )}
          {meta?.aborted && (
            <span className="text-[var(--accent-magenta)]">ABORTADO</span>
          )}
        </div>
        <div
          className={cn(
            'relative border border-[var(--border)] px-4 py-3 text-[13px] text-[var(--text)] leading-relaxed',
          )}
        >
          {/* corner brackets */}
          <span aria-hidden className="pointer-events-none absolute -top-px -left-px h-2 w-2 border-t border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -top-px -right-px h-2 w-2 border-t border-r border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -left-px h-2 w-2 border-b border-l border-[var(--accent-cyan)]" />
          <span aria-hidden className="pointer-events-none absolute -bottom-px -right-px h-2 w-2 border-b border-r border-[var(--accent-cyan)]" />

          {message.content.length === 0 && streaming ? (
            <MatrixLoader />
          ) : (
          <div
            className={cn(
              'prose-hefesto',
              streaming && message.content.length > 0 && 'streaming-cursor',
            )}
          >
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
                code: ({ className, children, ...props }) => {
                  const isInline = !className
                  if (isInline) {
                    return (
                      <code
                        className="font-mono text-[12px] px-1 py-[1px] bg-[var(--bg-overlay)] border border-[var(--border-dim)] text-[var(--accent-cyan)]"
                        {...props}
                      >
                        {children}
                      </code>
                    )
                  }
                  return (
                    <code className={cn('font-mono text-[12px]', className)} {...props}>
                      {children}
                    </code>
                  )
                },
                pre: ({ children }) => (
                  <pre className="my-2 p-3 bg-[var(--bg-overlay)] border border-[var(--border-dim)] overflow-x-auto font-mono text-[12px] text-[var(--text)]">
                    {children}
                  </pre>
                ),
                a: ({ children, href }) => (
                  <a
                    href={href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[var(--accent-cyan)] underline hover:no-underline"
                  >
                    {children}
                  </a>
                ),
                ul: ({ children }) => (
                  <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>
                ),
                ol: ({ children }) => (
                  <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol>
                ),
                h1: ({ children }) => (
                  <h1 className="font-display uppercase tracking-[0.15em] text-[var(--accent-cyan)] text-lg mb-2">
                    {children}
                  </h1>
                ),
                h2: ({ children }) => (
                  <h2 className="font-display uppercase tracking-[0.12em] text-[var(--accent-cyan)] text-base mb-2">
                    {children}
                  </h2>
                ),
                h3: ({ children }) => (
                  <h3 className="font-display uppercase tracking-[0.12em] text-[var(--text)] text-sm mb-1">
                    {children}
                  </h3>
                ),
              }}
            >
              {message.content}
            </ReactMarkdown>
          </div>
          )}
        </div>
      </div>
    </div>
  )
}
