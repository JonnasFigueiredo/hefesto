import { Frame } from '@/components/ui/Frame'
import { Panel } from '@/components/ui/Panel'
import { Spinner } from '@/components/ui/Spinner'
import { useTranslation } from '@/i18n/I18nProvider'
import { useUsageEvents, useUsageSummary, useUsageTimeseries } from '@/hooks/useUsage'
import type { UsageEvent } from '@/types/usage'
import { cn } from '@/lib/cn'

/**
 * Dashboard de telemetria. Lê /api/usage/summary, /api/usage/events e
 * /api/usage/timeseries. Sem libs de gráfico — barras CSS feitas à mão
 * pra manter a vibe FUI sem peso adicional.
 */
export function AnalyticsPage() {
  const { t } = useTranslation()
  const summary = useUsageSummary()
  const events = useUsageEvents(50)
  const timeseries = useUsageTimeseries(14)

  const totalEvents = summary.data?.totalEvents ?? 0
  const countByType = summary.data?.countByType ?? {}
  const avgByType = summary.data?.avgDurationMsByType ?? {}

  // Conta KPIs derivados a partir dos contadores por tipo.
  const chatComplete = countByType['chat.complete'] ?? 0
  const chatErrors = countByType['chat.error'] ?? 0
  const attachmentUploads = countByType['attachment.upload'] ?? 0
  const errorRate =
    totalEvents > 0 ? ((chatErrors / Math.max(1, chatComplete + chatErrors)) * 100).toFixed(1) : '0.0'

  // Latência média geral do chat
  const chatAvgLatency = avgByType['chat.complete']

  return (
    <div className="h-full flex flex-col gap-6 overflow-y-auto">
      <div className="flex items-center gap-3">
        <h1 className="font-display text-2xl uppercase tracking-[0.25em] text-[var(--accent-cyan)] glow-cyan">
          {t('analytics.title')}
        </h1>
        {summary.isLoading && <Spinner size={14} />}
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard label={t('analytics.kpiTotalEvents')} value={totalEvents.toLocaleString('pt-BR')} />
        <KpiCard label={t('analytics.kpiCompletedMessages')} value={chatComplete.toLocaleString('pt-BR')} accent="green" />
        <KpiCard label={t('analytics.kpiAttachmentUploads')} value={attachmentUploads.toLocaleString('pt-BR')} accent="cyan" />
        <KpiCard
          label={t('analytics.kpiErrorRate')}
          value={`${errorRate}%`}
          accent={chatErrors > 0 ? 'magenta' : 'dim'}
        />
      </div>

      {/* Latência média + breakdown por tipo */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Frame title={t('analytics.avgLatency')}>
          <Panel className="font-mono text-[12px]">
            {chatAvgLatency !== undefined ? (
              <>
                <div className="font-display text-[28px] tracking-[0.1em] text-[var(--accent-cyan)] glow-cyan">
                  {Math.round(chatAvgLatency).toLocaleString('pt-BR')} ms
                </div>
                <div className="text-[var(--text-muted)] mt-1">
                  {t('analytics.avgLatencyHint')}
                </div>
              </>
            ) : (
              <div className="text-[var(--text-muted)]">{t('analytics.noData')}</div>
            )}

            {Object.keys(avgByType).length > 1 && (
              <div className="mt-4 pt-4 border-t border-[var(--border-dim)] space-y-1">
                {Object.entries(avgByType)
                  .filter(([evType]) => evType !== 'chat.complete')
                  .map(([evType, ms]) => (
                    <div key={evType} className="flex items-center justify-between gap-3 text-[10px]">
                      <span className="text-[var(--text-dim)] truncate">{evType}</span>
                      <span className="text-[var(--text)]">
                        {Math.round(ms).toLocaleString('pt-BR')} ms
                      </span>
                    </div>
                  ))}
              </div>
            )}
          </Panel>
        </Frame>

        <Frame title={t('analytics.eventsByType')}>
          <BarChart data={countByType} />
        </Frame>
      </div>

      {/* Time series por dia */}
      <Frame title={t('analytics.activity14days')}>
        <DailyChart data={timeseries.data ?? []} />
      </Frame>

      {/* Lista de eventos recentes */}
      <Frame title={t('analytics.recentEvents')}>
        <RecentEvents events={events.data ?? []} />
      </Frame>
    </div>
  )
}

// --------------------------------------------------------------------------
// KPI card
// --------------------------------------------------------------------------

interface KpiProps {
  label: string
  value: string | number
  accent?: 'cyan' | 'green' | 'magenta' | 'amber' | 'dim'
}

function KpiCard({ label, value, accent = 'cyan' }: KpiProps) {
  const colorMap: Record<string, string> = {
    cyan: 'text-[var(--accent-cyan)]',
    green: 'text-[var(--accent-green)]',
    magenta: 'text-[var(--accent-magenta)]',
    amber: 'text-[var(--accent-amber)]',
    dim: 'text-[var(--text-dim)]',
  }
  const glowMap: Record<string, string> = {
    cyan: 'glow-cyan',
    green: '',
    magenta: '',
    amber: '',
    dim: '',
  }

  return (
    <div className="relative border border-[var(--border-dim)] p-4 bg-[var(--bg-elevated)]">
      <span aria-hidden className="pointer-events-none absolute -top-px -left-px h-2 w-2 border-t border-l border-[var(--accent-cyan)]" />
      <span aria-hidden className="pointer-events-none absolute -top-px -right-px h-2 w-2 border-t border-r border-[var(--accent-cyan)]" />
      <span aria-hidden className="pointer-events-none absolute -bottom-px -left-px h-2 w-2 border-b border-l border-[var(--accent-cyan)]" />
      <span aria-hidden className="pointer-events-none absolute -bottom-px -right-px h-2 w-2 border-b border-r border-[var(--accent-cyan)]" />

      <div className="font-mono text-[10px] text-[var(--text-muted)] uppercase tracking-[0.18em]">
        {label}
      </div>
      <div
        className={cn(
          'font-display text-[28px] tracking-[0.1em] mt-1',
          colorMap[accent],
          glowMap[accent],
        )}
      >
        {value}
      </div>
    </div>
  )
}

// --------------------------------------------------------------------------
// Bar chart (eventos por tipo)
// --------------------------------------------------------------------------

function BarChart({ data }: { data: Record<string, number> }) {
  const { t } = useTranslation()
  const entries = Object.entries(data).sort((a, b) => b[1] - a[1])
  if (entries.length === 0) {
    return (
      <div className="font-mono text-[10px] text-[var(--text-muted)] py-4">
        {t('analytics.noEventsYet')}
      </div>
    )
  }
  const max = Math.max(...entries.map(([, n]) => n))

  const colorOf = (eventType: string): string => {
    if (eventType.startsWith('chat.error')) return 'var(--accent-magenta)'
    if (eventType.startsWith('chat.complete')) return 'var(--accent-green)'
    if (eventType.startsWith('chat.')) return 'var(--accent-cyan)'
    if (eventType.startsWith('attachment.')) return 'var(--accent-amber)'
    return 'var(--text-dim)'
  }

  return (
    <div className="space-y-2">
      {entries.map(([type, count]) => {
        const pct = (count / max) * 100
        return (
          <div key={type} className="flex items-center gap-3 font-mono text-[11px]">
            <span className="w-44 text-[var(--text-dim)] truncate">{type}</span>
            <div className="flex-1 h-4 bg-[var(--bg-overlay)] relative">
              <div
                className="absolute inset-y-0 left-0 transition-all"
                style={{
                  width: `${pct}%`,
                  backgroundColor: colorOf(type),
                  boxShadow: `0 0 6px ${colorOf(type)}`,
                  opacity: 0.7,
                }}
              />
            </div>
            <span className="w-12 text-right text-[var(--text)]">{count}</span>
          </div>
        )
      })}
    </div>
  )
}

// --------------------------------------------------------------------------
// Daily chart — barras agrupadas por dia
// --------------------------------------------------------------------------

interface DailyData {
  day: string
  eventType: string
  count: number
}

function DailyChart({ data }: { data: DailyData[] }) {
  const { t } = useTranslation()
  if (data.length === 0) {
    return (
      <div className="font-mono text-[10px] text-[var(--text-muted)] py-4">
        {t('analytics.no14dData')}
      </div>
    )
  }

  // Agrupa por dia, soma totais
  const byDay = new Map<string, number>()
  for (const d of data) {
    byDay.set(d.day, (byDay.get(d.day) ?? 0) + d.count)
  }
  const days = Array.from(byDay.entries()).sort(([a], [b]) => a.localeCompare(b))
  const max = Math.max(...days.map(([, n]) => n))

  return (
    <div className="flex items-end gap-1 h-32">
      {days.map(([day, total]) => {
        const heightPct = max > 0 ? (total / max) * 100 : 0
        const dayLabel = day.slice(5) // mes-dia
        return (
          <div key={day} className="flex-1 flex flex-col items-center gap-1 group">
            <div className="text-[9px] font-mono text-[var(--text-muted)] opacity-0 group-hover:opacity-100 transition-opacity">
              {total}
            </div>
            <div
              className="w-full bg-[var(--accent-cyan)] transition-all"
              style={{
                height: `${Math.max(2, heightPct)}%`,
                opacity: 0.7,
                boxShadow: '0 0 6px var(--accent-cyan)',
              }}
              title={`${day}: ${total} eventos`}
            />
            <div className="text-[9px] font-mono text-[var(--text-dim)] tracking-tight">
              {dayLabel}
            </div>
          </div>
        )
      })}
    </div>
  )
}

// --------------------------------------------------------------------------
// Lista de eventos recentes
// --------------------------------------------------------------------------

function RecentEvents({ events }: { events: UsageEvent[] }) {
  const { t } = useTranslation()
  if (events.length === 0) {
    return (
      <div className="font-mono text-[10px] text-[var(--text-muted)] py-4">
        {t('analytics.noEvents')}
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full font-mono text-[11px]">
        <thead className="text-[var(--text-muted)] uppercase tracking-[0.18em]">
          <tr className="border-b border-[var(--border-dim)]">
            <th className="text-left py-2 px-2">{t('analytics.tableTimestamp')}</th>
            <th className="text-left py-2 px-2">{t('analytics.tableType')}</th>
            <th className="text-left py-2 px-2">{t('analytics.tableConv')}</th>
            <th className="text-right py-2 px-2">{t('analytics.tableDuration')}</th>
            <th className="text-left py-2 px-2 hidden md:table-cell">{t('analytics.tablePayload')}</th>
          </tr>
        </thead>
        <tbody>
          {events.map((e) => {
            const colorOf =
              e.eventType.includes('error')
                ? 'text-[var(--accent-magenta)]'
                : e.eventType.includes('complete')
                  ? 'text-[var(--accent-green)]'
                  : e.eventType.startsWith('attachment')
                    ? 'text-[var(--accent-amber)]'
                    : 'text-[var(--accent-cyan)]'
            return (
              <tr key={e.id} className="border-b border-[var(--border-dim)]">
                <td className="py-1.5 px-2 text-[var(--text-dim)] whitespace-nowrap">
                  {formatTime(e.timestamp)}
                </td>
                <td className={cn('py-1.5 px-2 whitespace-nowrap', colorOf)}>
                  {e.eventType}
                </td>
                <td className="py-1.5 px-2 text-[var(--text-muted)] truncate max-w-[80px]">
                  {e.conversationId ?? '—'}
                </td>
                <td className="py-1.5 px-2 text-right text-[var(--text)]">
                  {e.durationMs == null ? '—' : `${e.durationMs} ms`}
                </td>
                <td className="py-1.5 px-2 text-[var(--text-muted)] hidden md:table-cell max-w-md truncate">
                  {e.payloadJson ?? ''}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function formatTime(ts: number): string {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
