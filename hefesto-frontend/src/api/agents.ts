import { http } from './http'
import type { Agent } from '@/types/agent'

export function listAgents(): Promise<Agent[]> {
  return http<Agent[]>('/api/agents')
}
