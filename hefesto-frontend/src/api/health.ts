import { http } from './http'

export interface HealthResponse {
  status: 'ok' | string
  ts: number
  service: string
  version: string
}

export function fetchHealth(): Promise<HealthResponse> {
  return http<HealthResponse>('/api/health')
}
