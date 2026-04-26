import { useQuery } from '@tanstack/react-query'
import { listAgents } from '@/api/agents'

export function useAgents() {
  return useQuery({
    queryKey: ['agents'],
    queryFn: listAgents,
    staleTime: 60_000,
  })
}
