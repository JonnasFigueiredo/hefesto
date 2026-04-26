import { useMutation } from '@tanstack/react-query'
import { sendChat } from '@/api/chat'
import { renameConversationId, useChatStore } from '@/store/chatStore'

interface SendArgs {
  message: string
}

/**
 * Hook que dispara uma mensagem usando o adapter selecionado e a conversa ativa
 * (criando uma se não houver). Atualiza o store antes da requisição (mensagem
 * do usuário visível imediatamente) e ao final (resposta do assistente).
 */
export function useSendChat() {
  const mutation = useMutation({
    mutationFn: async ({ message }: SendArgs) => {
      const state = useChatStore.getState()
      if (!state.selectedAdapterId) {
        throw new Error('Nenhum adapter selecionado')
      }

      // Garante uma conversa ativa.
      let activeId = state.activeId
      if (!activeId) {
        activeId = state.createConversation(state.selectedAdapterId)
      }

      // Otimistamente adiciona a mensagem do usuário no store.
      state.appendMessage(activeId, {
        role: 'user',
        content: message,
        timestamp: Date.now(),
      })

      const isLocalId = activeId.startsWith('local-')
      const response = await sendChat({
        adapterId: state.selectedAdapterId,
        conversationId: isLocalId ? null : activeId,
        message,
      })

      // Backend retorna id real (UUID truncado); renomeia se era local-*.
      if (isLocalId && response.conversationId !== activeId) {
        renameConversationId(activeId, response.conversationId)
        activeId = response.conversationId
      }

      // Adiciona resposta do assistente.
      useChatStore.getState().appendMessage(activeId, {
        role: 'assistant',
        content: response.content,
        timestamp: Date.now(),
        meta: {
          latencyMs: response.latencyMs,
          model: response.model,
          adapterId: response.adapterId,
        },
      })

      return response
    },
  })

  return mutation
}
