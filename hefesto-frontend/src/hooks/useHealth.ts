import { useQuery } from '@tanstack/react-query'
import { fetchHealth } from '@/api/health'

export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    refetchInterval: 10_000,
    refetchOnWindowFocus: true,
    retry: 1,
    staleTime: 5_000,
  })
}
