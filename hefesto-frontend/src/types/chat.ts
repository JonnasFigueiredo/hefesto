export type MessageRole = 'user' | 'assistant' | 'system' | 'jira_context'

export interface Message {
  id: string
  role: MessageRole
  content: string
  timestamp: number
  /** Metadata da resposta do assistente: latência, modelo, etc. */
  meta?: {
    latencyMs?: number
    model?: string | null
    adapterId?: string
    aborted?: boolean
  }
}

/**
 * Contexto persistente da conversa: agente, arquivos anexados, issue do Jira.
 * Reaplicado a cada mensagem (Claude Code é stateless por chamada).
 */
export interface ConversationContext {
  agentId: string
  attachmentIds: string[]
  jiraIssueKey: string | null
}

export interface Conversation {
  id: string
  title: string | null
  adapterId: string
  context: ConversationContext
  createdAt: number
  updatedAt: number
  messages: Message[]
}

export interface SendChatRequest {
  adapterId: string
  conversationId: string | null
  message: string
}

export interface SendChatResponse {
  conversationId: string
  content: string
  adapterId: string
  latencyMs: number
  model: string | null
}
