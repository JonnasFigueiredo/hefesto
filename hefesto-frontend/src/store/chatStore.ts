import { create } from 'zustand'
import type {
  Conversation,
  ConversationContext,
  Message,
  MessageRole,
} from '@/types/chat'

interface ChatState {
  conversations: Conversation[]
  activeId: string | null
  selectedAdapterId: string | null
  /** Id da mensagem atualmente recebendo chunks (assistant). */
  streamingMessageId: string | null

  setSelectedAdapter: (id: string) => void
  setActive: (id: string | null) => void

  /** Cria uma nova conversa local (id provisório) e ativa. */
  createConversation: (
    adapterId: string,
    initialContext?: Partial<ConversationContext>,
  ) => string

  /** Garante que uma conversa exista; usado quando o backend retorna um id novo. */
  upsertConversation: (id: string, adapterId: string) => void

  appendMessage: (conversationId: string, msg: Omit<Message, 'id'>) => string

  /** Cria uma mensagem assistant vazia que receberá chunks. Retorna id. */
  startStreaming: (conversationId: string, adapterId: string) => string

  /** Anexa texto à mensagem em streaming. */
  appendChunk: (conversationId: string, content: string) => void

  /** Finaliza streaming, atribui meta + serverMessageId. */
  completeStreaming: (
    conversationId: string,
    finalContent: string,
    meta: { latencyMs?: number; model?: string | null; adapterId?: string; serverMessageId?: string | null },
  ) => void

  /** Cancela streaming, mantém o que já chegou. */
  cancelStreaming: (conversationId: string) => void

  removeConversation: (id: string) => void

  /**
   * Substitui o estado local pelas conversas vindas do backend.
   * Chamado uma vez ao montar a ChatPage pra hidratar do SQLite.
   */
  loadFromServer: (
    serverConversations: Array<{
      id: string
      title: string | null
      adapterId: string
      agentId: string
      jiraIssueKey: string | null
      attachmentIds: string[]
      createdAt: number
      updatedAt: number
      messages: Array<{ id: string; role: string; content: string; timestamp: number }>
    }>,
  ) => void

  // ---- Context manipulations ----

  setAgent: (conversationId: string, agentId: string) => void
  addAttachment: (conversationId: string, attachmentId: string) => void
  removeAttachment: (conversationId: string, attachmentId: string) => void
  setJiraIssue: (conversationId: string, key: string | null) => void

  clearAll: () => void
}

const localId = () =>
  globalThis.crypto?.randomUUID?.() ?? Math.random().toString(36).slice(2, 10)

const messageId = () => localId().slice(0, 12)

const defaultContext = (
  partial?: Partial<ConversationContext>,
): ConversationContext => ({
  agentId: partial?.agentId ?? 'default',
  attachmentIds: partial?.attachmentIds ?? [],
  jiraIssueKey: partial?.jiraIssueKey ?? null,
})

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeId: null,
  selectedAdapterId: null,
  streamingMessageId: null,

  setSelectedAdapter: (id) => set({ selectedAdapterId: id }),
  setActive: (id) => set({ activeId: id }),

  createConversation: (adapterId, initialContext) => {
    const id = `local-${localId().slice(0, 8)}`
    const now = Date.now()
    const conv: Conversation = {
      id,
      title: null,
      adapterId,
      context: defaultContext(initialContext),
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
      context: defaultContext(),
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
    const { serverMessageId, ...rest } = meta
    set((s) => ({
      conversations: s.conversations.map((c) => {
        if (c.id !== conversationId) return c
        return {
          ...c,
          messages: c.messages.map((m) =>
            m.id === targetId
              ? {
                  ...m,
                  content: finalContent,
                  serverMessageId: serverMessageId ?? m.serverMessageId ?? null,
                  meta: { ...m.meta, ...rest },
                }
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

  loadFromServer: (serverConversations) => {
    if (!serverConversations || serverConversations.length === 0) return
    const mapped: Conversation[] = serverConversations.map((s) => ({
      id: s.id,
      title: s.title,
      adapterId: s.adapterId,
      context: {
        agentId: s.agentId ?? 'default',
        attachmentIds: s.attachmentIds ?? [],
        jiraIssueKey: s.jiraIssueKey,
      },
      createdAt: s.createdAt,
      updatedAt: s.updatedAt,
      messages: (s.messages ?? []).map((m) => ({
        id: messageId(),
        serverMessageId: m.id,
        role: (m.role as Message['role']),
        content: m.content,
        timestamp: m.timestamp,
      })),
    }))

    set((curr) => {
      // Mantém qualquer conversa local-* não sincronizada ainda no topo.
      const localOnly = curr.conversations.filter((c) => c.id.startsWith('local-'))
      const merged = [...localOnly, ...mapped]
      // Se não tinha activeId ou apontava pra algo que não existe mais, escolhe a mais recente.
      const stillExists = curr.activeId
        ? merged.some((c) => c.id === curr.activeId)
        : false
      const activeId = stillExists ? curr.activeId : merged[0]?.id ?? null
      return { conversations: merged, activeId }
    })
  },

  setAgent: (conversationId, agentId) => {
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === conversationId
          ? { ...c, context: { ...c.context, agentId } }
          : c,
      ),
    }))
  },

  addAttachment: (conversationId, attachmentId) => {
    set((s) => ({
      conversations: s.conversations.map((c) => {
        if (c.id !== conversationId) return c
        if (c.context.attachmentIds.includes(attachmentId)) return c
        return {
          ...c,
          context: {
            ...c.context,
            attachmentIds: [...c.context.attachmentIds, attachmentId],
          },
        }
      }),
    }))
  },

  removeAttachment: (conversationId, attachmentId) => {
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === conversationId
          ? {
              ...c,
              context: {
                ...c.context,
                attachmentIds: c.context.attachmentIds.filter(
                  (id) => id !== attachmentId,
                ),
              },
            }
          : c,
      ),
    }))
  },

  setJiraIssue: (conversationId, key) => {
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === conversationId
          ? { ...c, context: { ...c.context, jiraIssueKey: key } }
          : c,
      ),
    }))
  },

  clearAll: () =>
    set({ conversations: [], activeId: null, streamingMessageId: null }),
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
