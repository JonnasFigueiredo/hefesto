import { create } from 'zustand'
import type { Conversation, Message, MessageRole } from '@/types/chat'

interface ChatState {
  conversations: Conversation[]
  activeId: string | null
  selectedAdapterId: string | null
  /** Id da mensagem atualmente recebendo chunks (assistant). */
  streamingMessageId: string | null

  setSelectedAdapter: (id: string) => void
  setActive: (id: string | null) => void

  /** Cria uma nova conversa local (id provisório) e ativa. */
  createConversation: (adapterId: string) => string

  /** Garante que uma conversa exista; usado quando o backend retorna um id novo. */
  upsertConversation: (id: string, adapterId: string) => void

  appendMessage: (conversationId: string, msg: Omit<Message, 'id'>) => string

  /** Cria uma mensagem assistant vazia que receberá chunks. Retorna id. */
  startStreaming: (conversationId: string, adapterId: string) => string

  /** Anexa texto à mensagem em streaming. */
  appendChunk: (conversationId: string, content: string) => void

  /** Finaliza streaming, atribui meta. */
  completeStreaming: (
    conversationId: string,
    finalContent: string,
    meta: { latencyMs?: number; model?: string | null; adapterId?: string },
  ) => void

  /** Cancela streaming, mantém o que já chegou. */
  cancelStreaming: (conversationId: string) => void

  removeConversation: (id: string) => void

  clearAll: () => void
}

const localId = () =>
  globalThis.crypto?.randomUUID?.() ?? Math.random().toString(36).slice(2, 10)

const messageId = () => localId().slice(0, 12)

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeId: null,
  selectedAdapterId: null,
  streamingMessageId: null,

  setSelectedAdapter: (id) => set({ selectedAdapterId: id }),

  setActive: (id) => set({ activeId: id }),

  createConversation: (adapterId) => {
    const id = `local-${localId().slice(0, 8)}`
    const now = Date.now()
    const conv: Conversation = {
      id,
      title: null,
      adapterId,
      createdAt: now,
      updatedAt: now,
      messages: [],
    }
    set((s) => ({
      conversations: [conv, ...s.conversations],
      activeId: id,
    }))
    return id
  },

  upsertConversation: (id, adapterId) => {
    const exists = get().conversations.find((c) => c.id === id)
    if (exists) return
    const now = Date.now()
    const conv: Conversation = {
      id,
      title: null,
      adapterId,
      createdAt: now,
      updatedAt: now,
      messages: [],
    }
    set((s) => ({ conversations: [conv, ...s.conversations] }))
  },

  appendMessage: (conversationId, msg) => {
    const newId = messageId()
    const newMsg: Message = { id: newId, ...msg }
    set((s) => ({
      conversations: s.conversations.map((c) => {
        if (c.id !== conversationId) return c
        const messages = [...c.messages, newMsg]
        const title =
          c.title ??
          (newMsg.role === 'user'
            ? newMsg.content.length > 60
              ? newMsg.content.slice(0, 60) + '...'
              : newMsg.content
            : null)
        return { ...c, title, messages, updatedAt: Date.now() }
      }),
    }))
    return newId
  },

  startStreaming: (conversationId, adapterId) => {
    const newId = messageId()
    const placeholder: Message = {
      id: newId,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      meta: { adapterId },
    }
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === conversationId
          ? { ...c, messages: [...c.messages, placeholder], updatedAt: Date.now() }
          : c,
      ),
      streamingMessageId: newId,
    }))
    return newId
  },

  appendChunk: (conversationId, content) => {
    const targetId = get().streamingMessageId
    if (!targetId) return
    set((s) => ({
      conversations: s.conversations.map((c) => {
        if (c.id !== conversationId) return c
        return {
          ...c,
          messages: c.messages.map((m) =>
            m.id === targetId ? { ...m, content: m.content + content } : m,
          ),
          updatedAt: Date.now(),
        }
      }),
    }))
  },

  completeStreaming: (conversationId, finalContent, meta) => {
    const targetId = get().streamingMessageId
    if (!targetId) return
    set((s) => ({
      conversations: s.conversations.map((c) => {
        if (c.id !== conversationId) return c
        return {
          ...c,
          messages: c.messages.map((m) =>
            m.id === targetId
              ? { ...m, content: finalContent, meta: { ...m.meta, ...meta } }
              : m,
          ),
          updatedAt: Date.now(),
        }
      }),
      streamingMessageId: null,
    }))
  },

  cancelStreaming: (conversationId) => {
    const targetId = get().streamingMessageId
    if (!targetId) {
      set({ streamingMessageId: null })
      return
    }
    set((s) => ({
      conversations: s.conversations.map((c) => {
        if (c.id !== conversationId) return c
        // Mantém o que já chegou. Marca meta com aborted=true.
        return {
          ...c,
          messages: c.messages.map((m) =>
            m.id === targetId
              ? { ...m, meta: { ...m.meta, aborted: true } }
              : m,
          ),
        }
      }),
      streamingMessageId: null,
    }))
  },

  removeConversation: (id) => {
    set((s) => {
      const remaining = s.conversations.filter((c) => c.id !== id)
      const activeId = s.activeId === id ? (remaining[0]?.id ?? null) : s.activeId
      return { conversations: remaining, activeId }
    })
  },

  clearAll: () => set({ conversations: [], activeId: null, streamingMessageId: null }),
}))

/** Helper: obtém a conversa ativa. */
export function useActiveConversation(): Conversation | null {
  return useChatStore((s) => {
    if (!s.activeId) return null
    return s.conversations.find((c) => c.id === s.activeId) ?? null
  })
}

/** Util: rename conversation id (quando o backend retorna o id real). */
export function renameConversationId(oldId: string, newId: string) {
  useChatStore.setState((s) => ({
    conversations: s.conversations.map((c) =>
      c.id === oldId ? { ...c, id: newId } : c,
    ),
    activeId: s.activeId === oldId ? newId : s.activeId,
  }))
}

export type { MessageRole }
