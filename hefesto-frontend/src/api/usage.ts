import { http } from './http'
import type { DailyCount, UsageEvent, UsageSummary } from '@/types/usage'

export function getUsageSummary(): Promise<UsageSummary> {
  return http<UsageSummary>('/api/usage/summary')
}

export function getUsageEvents(limit = 100, type?: string): Promise<UsageEvent[]> {
  const qs = new URLSearchParams({ limit: String(limit) })
  if (type) qs.set('type', type)
  return http<UsageEvent[]>(`/api/usage/events?${qs.toString()}`)
}

export function getUsageTimeseries(days = 30): Promise<DailyCount[]> {
  return http<DailyCount[]>(`/api/usage/timeseries?days=${days}`)
}
