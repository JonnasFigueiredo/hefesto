package com.hefesto.chat.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefesto.chat.Conversation;
import com.hefesto.chat.ConversationStore;
import com.hefesto.llm.ChatChunk;
import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.ChatStreamHandler;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterRegistry;
import com.hefesto.llm.Message;

/**
 * Handler do endpoint /ws/chat. Gerencia uma conexão por cliente, suporta
 * múltiplas mensagens em sequência (mas não em paralelo na mesma sessão).
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final LlmAdapterRegistry registry;
    private final ConversationStore store;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Pool dedicada para spawn de subprocessos sem bloquear a thread do WS. */
    private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "chat-ws-worker");
        t.setDaemon(true);
        return t;
    });

    /** Flag de abort por sessão WS. */
    private final Map<String, AtomicBoolean> abortFlags = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(LlmAdapterRegistry registry, ConversationStore store) {
        this.registry = registry;
        this.store = store;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("WS connection established: {}", session.getId());
        abortFlags.put(session.getId(), new AtomicBoolean(false));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.debug("WS closed: {} ({})", session.getId(), status);
        AtomicBoolean flag = abortFlags.remove(session.getId());
        if (flag != null) flag.set(true);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WsMessages.In in;
        try {
            in = mapper.readValue(message.getPayload(), WsMessages.In.class);
        } catch (Exception e) {
            sendError(session, null, "Payload inválido: " + e.getMessage());
            return;
        }

        switch (in.type() == null ? "" : in.type()) {
            case "start" -> handleStart(session, in);
            case "abort" -> handleAbort(session, in);
            default -> sendError(session, in.conversationId(), "Tipo desconhecido: " + in.type());
        }
    }

    private void handleStart(WebSocketSession session, WsMessages.In in) {
        if (in.adapterId() == null || in.adapterId().isBlank()) {
            sendError(session, in.conversationId(), "adapterId é obrigatório");
            return;
        }
        if (in.message() == null || in.message().isBlank()) {
            sendError(session, in.conversationId(), "message não pode ser vazio");
            return;
        }

        LlmAdapter adapter = registry.get(in.adapterId()).orElse(null);
        if (adapter == null) {
            sendError(session, in.conversationId(), "Adapter desconhecido: " + in.adapterId());
            return;
        }
        if (!adapter.isAvailable()) {
            sendError(session, in.conversationId(),
                "Adapter '" + adapter.displayName() + "' indisponível no momento");
            return;
        }

        Conversation conv = store.getOrCreate(in.conversationId(), adapter.id());
        conv.setAdapterId(adapter.id());
        conv.addMessage(Message.user(in.message()));

        // Reset do flag de abort pra esta conversa.
        AtomicBoolean abortFlag = abortFlags.computeIfAbsent(
            session.getId(), k -> new AtomicBoolean(false));
        abortFlag.set(false);

        sendJson(session, WsMessages.OutStarted.of(conv.id(), adapter.id()));

        // Snapshot do histórico anterior à mensagem atual.
        var history = conv.messages().subList(0, conv.messages().size() - 1);
        ChatRequest req = new ChatRequest(conv.id(), history, in.message(), null);

        final String convId = conv.id();
        final LlmAdapter selectedAdapter = adapter;

        workers.submit(() -> {
            ChatStreamHandler handler = new ChatStreamHandler() {
                @Override
                public void onChunk(ChatChunk chunk) {
                    if (chunk.content() != null && !chunk.content().isEmpty()) {
                        sendJson(session, WsMessages.OutChunk.of(convId, chunk.content()));
                    }
                }

                @Override
                public void onComplete(ChatResponse response) {
                    Conversation c = store.get(convId);
                    if (c != null) {
                        c.addMessage(Message.assistant(response.content()));
                    }
                    sendJson(session, WsMessages.OutDone.of(
                        convId,
                        response.content(),
                        response.adapterId(),
                        response.model(),
                        response.latencyMs()
                    ));
                }

                @Override
                public void onError(Throwable error) {
                    log.warn("Stream error for conv={}", convId, error);
                    sendError(session, convId, error.getMessage());
                }

                @Override
                public boolean isAborted() {
                    return abortFlag.get() || !session.isOpen();
                }
            };

            try {
                selectedAdapter.chatStream(req, handler);
            } catch (Throwable t) {
                log.error("Unhandled error in adapter.chatStream", t);
                sendError(session, convId, "Erro inesperado: " + t.getMessage());
            }
        });
    }

    private void handleAbort(WebSocketSession session, WsMessages.In in) {
        AtomicBoolean flag = abortFlags.get(session.getId());
        if (flag != null) {
            flag.set(true);
            log.debug("Abort signaled for session {} (conv={})", session.getId(), in.conversationId());
        }
    }

    private void sendError(WebSocketSession session, String convId, String message) {
        sendJson(session, WsMessages.OutError.of(convId, message));
    }

    private synchronized void sendJson(WebSocketSession session, Object payload) {
        if (!session.isOpen()) return;
        try {
            String json = mapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("Failed to send WS message: {}", e.getMessage());
        }
    }
}
