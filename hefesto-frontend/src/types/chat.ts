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
  }
}

export interface Conversation {
  id: string
  title: string | null
  adapterId: string
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
