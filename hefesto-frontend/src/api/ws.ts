/**
 * Cliente WebSocket pro endpoint /ws/chat.
 *
 * Reconnect: tentativa automática com backoff exponencial até 30s.
 * Mensagens: JSON serializado/deserializado nas extremidades.
 */

export type WsState = 'connecting' | 'open' | 'closed'

export interface WsOutgoing {
  type: 'start' | 'abort'
  adapterId?: string
  conversationId?: string | null
  message?: string
  agentId?: string
  attachmentIds?: string[]
  jiraIssueKey?: string | null
}

export interface WsIncomingChunk {
  type: 'chunk'
  conversationId: string
  content: string
}

export interface WsIncomingDone {
  type: 'done'
  conversationId: string
  messageId: string | null
  content: string
  adapterId: string
  model: string | null
  latencyMs: number
}

export interface WsIncomingError {
  type: 'error'
  conversationId: string | null
  message: string
}

export interface WsIncomingStarted {
  type: 'started'
  conversationId: string
  adapterId: string
}

export type WsIncoming =
  | WsIncomingChunk
  | WsIncomingDone
  | WsIncomingError
  | WsIncomingStarted

interface ChatWsClient {
  send: (msg: WsOutgoing) => void
  close: () => void
  state: () => WsState
}

interface ClientOptions {
  onMessage: (msg: WsIncoming) => void
  onStateChange?: (state: WsState) => void
  url?: string
}

function resolveWsUrl(): string {
  // Em dev o Vite proxy intermedia /ws/* → backend:8080.
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/chat`
}

export function createChatWsClient(options: ClientOptions): ChatWsClient {
  const url = options.url ?? resolveWsUrl()
  let socket: WebSocket | null = null
  let state: WsState = 'connecting'
  let manualClose = false
  let reconnectAttempts = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  const queue: WsOutgoing[] = []

  const setState = (next: WsState) => {
    state = next
    options.onStateChange?.(next)
  }

  const flush = () => {
    while (socket && socket.readyState === WebSocket.OPEN && queue.length > 0) {
      const msg = queue.shift()!
      socket.send(JSON.stringify(msg))
    }
  }

  const connect = () => {
    setState('connecting')
    socket = new WebSocket(url)

    socket.onopen = () => {
      reconnectAttempts = 0
      setState('open')
      flush()
    }

    socket.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data) as WsIncoming
        options.onMessage(parsed)
      } catch (e) {
        console.warn('[hefesto:ws] failed to parse message', event.data, e)
      }
    }

    socket.onclose = () => {
      setState('closed')
      socket = null
      if (manualClose) return
      const delayMs = Math.min(30_000, 500 * Math.pow(2, reconnectAttempts))
      reconnectAttempts += 1
      reconnectTimer = setTimeout(connect, delayMs)
    }

    socket.onerror = (e) => {
      console.warn('[hefesto:ws] error', e)
    }
  }

  connect()

  return {
    send(msg) {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify(msg))
      } else {
        queue.push(msg)
      }
    },
    close() {
      manualClose = true
      if (reconnectTimer) clearTimeout(reconnectTimer)
      socket?.close()
    },
    state() {
      return state
    },
  }
}
