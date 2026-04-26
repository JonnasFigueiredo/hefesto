package com.hefesto.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hefesto.chat.dto.ChatRequestDto;
import com.hefesto.chat.dto.ChatResponseDto;
import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;
import com.hefesto.llm.LlmAdapterRegistry;
import com.hefesto.llm.Message;

/**
 * Orquestra o envio de uma mensagem: resolve o adapter, atualiza a conversa,
 * dispara o chat e retorna a resposta DTO.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final LlmAdapterRegistry registry;
    private final ConversationStore store;

    public ChatService(LlmAdapterRegistry registry, ConversationStore store) {
        this.registry = registry;
        this.store = store;
    }

    public ChatResponseDto sendMessage(ChatRequestDto req) {
        String adapterId = req.adapterId();
        if (adapterId == null || adapterId.isBlank()) {
            throw new LlmAdapterException("adapterId é obrigatório");
        }
        if (req.message() == null || req.message().isBlank()) {
            throw new LlmAdapterException("message não pode ser vazio");
        }

        LlmAdapter adapter = registry.get(adapterId)
            .orElseThrow(() -> new LlmAdapterException(
                "Adapter desconhecido: " + adapterId));

        if (!adapter.isAvailable()) {
            throw new LlmAdapterException(
                "Adapter '" + adapter.displayName() + "' indisponível no momento");
        }

        Conversation conv = store.getOrCreate(req.conversationId(), adapterId);
        conv.setAdapterId(adapterId);

        Message userMsg = Message.user(req.message());
        conv.addMessage(userMsg);

        log.debug("Dispatching chat: adapter={} conv={} historySize={}",
            adapterId, conv.id(), conv.messages().size() - 1);

        ChatRequest llmRequest = new ChatRequest(
            conv.id(),
            // Histórico: tudo que veio antes da mensagem nova.
            conv.messages().subList(0, conv.messages().size() - 1),
            req.message(),
            null
        );

        ChatResponse response = adapter.chat(llmRequest);

        Message assistantMsg = Message.assistant(response.content());
        conv.addMessage(assistantMsg);

        return new ChatResponseDto(
            conv.id(),
            response.content(),
            response.adapterId(),
            response.latencyMs(),
            response.model()
        );
    }
}
