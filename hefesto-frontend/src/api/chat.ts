import { http } from './http'
import type { SendChatRequest, SendChatResponse } from '@/types/chat'

export function sendChat(request: SendChatRequest): Promise<SendChatResponse> {
  return http<SendChatResponse>('/api/chat', {
    method: 'POST',
    body: JSON.stringify({
      adapterId: request.adapterId,
      conversationId: request.conversationId,
      message: request.message,
    }),
  })
}
