import { useEffect, useRef, useState } from 'react'
import { createChatWsClient, type WsState } from '@/api/ws'
import { renameConversationId, useChatStore } from '@/store/chatStore'

interface UseChatStream {
  /** Estado da conexão WebSocket. */
  wsState: WsState
  /** Tem streaming acontecendo agora? */
  isStreaming: boolean
  /** Última mensagem de erro recebida (string vazia = sem erro). */
  error: string | null
  /** Envia uma mensagem do usuário. Cria conversa se necessário. */
  send: (text: string) => void
  /** Aborta streaming em curso. */
  abort: () => void
  /** Limpa o erro corrente. */
  clearError: () => void
}

/**
 * Mantém uma conexão WebSocket única em background e expõe controles de
 * envio/abort. O streaming dos chunks atualiza o store automaticamente.
 */
export function useChatStream(): UseChatStream {
  const [wsState, setWsState] = useState<WsState>('connecting')
  const [error, setError] = useState<string | null>(null)
  const clientRef = useRef<ReturnType<typeof createChatWsClient> | null>(null)
  // Mapa local pra renomear conversaId após o backend confirmar.
  const pendingLocalIdRef = useRef<string | null>(null)

  useEffect(() => {
    const client = createChatWsClient({
      onStateChange: setWsState,
      onMessage: (msg) => {
        switch (msg.type) {
          case 'started': {
            // Backend confirmou e atribuiu o id real (UUID).
            const localId = pendingLocalIdRef.current
            if (localId && localId !== msg.conversationId) {
              renameConversationId(localId, msg.conversationId)
            }
            pendingLocalIdRef.current = null
            // Cria placeholder de assistant pra receber chunks.
            useChatStore.getState().startStreaming(msg.conversationId, msg.adapterId)
            break
          }
          case 'chunk': {
            useChatStore.getState().appendChunk(msg.conversationId, msg.content)
            break
          }
          case 'done': {
            useChatStore.getState().completeStreaming(
              msg.conversationId,
              msg.content,
              {
                latencyMs: msg.latencyMs,
                model: msg.model,
                adapterId: msg.adapterId,
              },
            )
            break
          }
          case 'error': {
            setError(msg.message)
            // Se estava streaming, cancela mantendo o conteúdo parcial.
            const convId = msg.conversationId ?? useChatStore.getState().activeId
            if (convId) {
              useChatStore.getState().cancelStreaming(convId)
            }
            break
          }
        }
      },
    })
    clientRef.current = client
    return () => client.close()
  }, [])

  const send = (text: string) => {
    setError(null)
    const state = useChatStore.getState()
    if (!state.selectedAdapterId) {
      setError('Nenhum adapter selecionado')
      return
    }
    if (state.streamingMessageId) {
      setError('Já existe um streaming em andamento — aborte antes de enviar')
      return
    }

    let activeId = state.activeId
    if (!activeId) {
      activeId = state.createConversation(state.selectedAdapterId)
    }

    // Adiciona mensagem do usuário no store (otimista).
    state.appendMessage(activeId, {
      role: 'user',
      content: text,
      timestamp: Date.now(),
    })

    const isLocalId = activeId.startsWith('local-')
    pendingLocalIdRef.current = isLocalId ? activeId : null

    clientRef.current?.send({
      type: 'start',
      adapterId: state.selectedAdapterId,
      conversationId: isLocalId ? null : activeId,
      message: text,
    })
  }

  const abort = () => {
    const state = useChatStore.getState()
    const convId = state.activeId
    if (!convId) return
    clientRef.current?.send({ type: 'abort', conversationId: convId })
    state.cancelStreaming(convId)
  }

  const isStreaming = useChatStore((s) => s.streamingMessageId !== null)

  return {
    wsState,
    isStreaming,
    error,
    send,
    abort,
    clearError: () => setError(null),
  }
}
