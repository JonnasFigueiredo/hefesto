import { http } from './http'

export interface ConversationServer {
  id: string
  title: string | null
  adapterId: string
  agentId: string
  jiraIssueKey: string | null
  attachmentIds: string[]
  createdAt: number
  updatedAt: number
  archived: boolean
  messages: Array<{
    role: string
    content: string
    timestamp: number
  }>
}

export function listConversations(): Promise<ConversationServer[]> {
  return http<ConversationServer[]>('/api/conversations')
}

export function getConversation(id: string): Promise<ConversationServer> {
  return http<ConversationServer>(`/api/conversations/${encodeURIComponent(id)}`)
}

export async function deleteConversation(id: string): Promise<void> {
  const res = await fetch(`/api/conversations/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  if (!res.ok && res.status !== 404) {
    throw new Error(`Falha ao deletar conversa (${res.status})`)
  }
}
