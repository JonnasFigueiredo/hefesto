package com.hefesto.chat.ws;

import java.io.IOException;
import java.util.Collections;
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
import com.hefesto.chat.ChatService;
import com.hefesto.chat.Conversation;
import com.hefesto.chat.ConversationStore;
import com.hefesto.chat.dto.ChatRequestDto;
import com.hefesto.llm.ChatChunk;
import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.ChatStreamHandler;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.Message;

/**
 * Handler do endpoint /ws/chat. Gerencia uma conexão por cliente, suporta
 * múltiplas mensagens em sequência (mas não em paralelo na mesma sessão).
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final ConversationStore store;
    private final com.hefesto.testcases.TestCaseService testCases;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Pool dedicada para spawn de subprocessos sem bloquear a thread do WS. */
    private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "chat-ws-worker");
        t.setDaemon(true);
        return t;
    });

    /** Flag de abort por sessão WS. */
    private final Map<String, AtomicBoolean> abortFlags = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(
        ChatService chatService,
        ConversationStore store,
        com.hefesto.testcases.TestCaseService testCases
    ) {
        this.chatService = chatService;
        this.store = store;
        this.testCases = testCases;
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

        LlmAdapter adapter;
        try {
            adapter = chatService.resolveAdapter(in.adapterId());
        } catch (Exception e) {
            sendError(session, in.conversationId(), e.getMessage());
            return;
        }

        Conversation conv = store.getOrCreate(in.conversationId(), adapter.id());
        conv.setAdapterId(adapter.id());
        if (in.agentId() != null && !in.agentId().isBlank()) conv.setAgentId(in.agentId());
        if (in.jiraIssueKey() != null) conv.setJiraIssueKey(in.jiraIssueKey());
        store.save(conv);

        if (in.attachmentIds() != null && !in.attachmentIds().isEmpty()) {
            store.linkAttachments(conv.id(), in.attachmentIds());
        }

        store.appendMessage(conv.id(), Message.user(in.message()));

        // Reset do flag de abort pra esta conversa.
        AtomicBoolean abortFlag = abortFlags.computeIfAbsent(
            session.getId(), k -> new AtomicBoolean(false));
        abortFlag.set(false);

        sendJson(session, WsMessages.OutStarted.of(conv.id(), adapter.id()));

        // Constrói o prompt em camadas via ChatService (mesmas regras do REST).
        ChatRequestDto reqDto = new ChatRequestDto(
            in.adapterId(),
            conv.id(),
            in.message(),
            in.agentId(),
            in.attachmentIds(),
            in.jiraIssueKey()
        );
        String layeredPrompt;
        try {
            layeredPrompt = chatService.buildLayeredPrompt(reqDto);
        } catch (Exception e) {
            log.warn("Failed to build layered prompt", e);
            sendError(session, conv.id(), "Erro ao montar prompt: " + e.getMessage());
            return;
        }

        log.debug("Layered prompt length: {} chars (agent={}, attachments={}, jira={})",
            layeredPrompt.length(),
            in.agentId(),
            in.attachmentIds() == null ? 0 : in.attachmentIds().size(),
            in.jiraIssueKey());

        ChatRequest req = new ChatRequest(
            conv.id(),
            Collections.emptyList(),
            layeredPrompt,
            null
        );

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
                    String storedId = null;
                    Conversation c = store.get(convId);
                    if (c != null) {
                        var stored = store.appendMessage(
                            convId, Message.assistant(response.content()));
                        storedId = stored.id();
                        // Auto-extração de test cases (rodando QA Specialist?).
                        testCases.extractAndSave(
                            in.agentId(),
                            convId,
                            storedId,
                            response.content()
                        );
                    }
                    sendJson(session, WsMessages.OutDone.of(
                        convId,
                        storedId,
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
