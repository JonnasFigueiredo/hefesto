import { useQuery } from '@tanstack/react-query'
import {
  getUsageEvents,
  getUsageSummary,
  getUsageTimeseries,
} from '@/api/usage'

export function useUsageSummary() {
  return useQuery({
    queryKey: ['usage', 'summary'],
    queryFn: getUsageSummary,
    staleTime: 10_000,
    refetchInterval: 15_000,
  })
}

export function useUsageEvents(limit = 50, type?: string) {
  return useQuery({
    queryKey: ['usage', 'events', limit, type ?? null],
    queryFn: () => getUsageEvents(limit, type),
    staleTime: 10_000,
    refetchInterval: 15_000,
  })
}

export function useUsageTimeseries(days = 30) {
  return useQuery({
    queryKey: ['usage', 'timeseries', days],
    queryFn: () => getUsageTimeseries(days),
    staleTime: 30_000,
  })
}
