import { http } from './http'
import type { Adapter } from '@/types/adapter'

export function listAdapters(): Promise<Adapter[]> {
  return http<Adapter[]>('/api/llm/adapters')
}
