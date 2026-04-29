export interface UsageEvent {
  id: number
  timestamp: number
  eventType: string
  conversationId: string | null
  payloadJson: string | null
  durationMs: number | null
}

export interface UsageSummary {
  totalEvents: number
  countByType: Record<string, number>
  avgDurationMsByType: Record<string, number>
}

export interface DailyCount {
  day: string
  eventType: string
  count: number
}
