import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listAdapters } from '@/api/adapters'
import { useChatStore } from '@/store/chatStore'

export function useAdapters() {
  const query = useQuery({
    queryKey: ['adapters'],
    queryFn: listAdapters,
    staleTime: 30_000,
    refetchInterval: 30_000,
  })

  const setSelectedAdapter = useChatStore((s) => s.setSelectedAdapter)
  const selectedAdapterId = useChatStore((s) => s.selectedAdapterId)

  // Auto-seleciona o primeiro adapter disponível se nenhum foi escolhido.
  useEffect(() => {
    if (selectedAdapterId) return
    const first = query.data?.find((a) => a.available) ?? query.data?.[0]
    if (first) setSelectedAdapter(first.id)
  }, [query.data, selectedAdapterId, setSelectedAdapter])

  return query
}
