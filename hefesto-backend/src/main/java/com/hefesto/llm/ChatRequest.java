package com.hefesto.llm;

import java.util.List;
import java.util.Map;

/**
 * Entrada do método {@link LlmAdapter#chat(ChatRequest)}.
 *
 * @param conversationId  identificador opaco da conversa (gerado pelo cliente).
 * @param history         mensagens anteriores; pode estar vazio para início.
 * @param userMessage     mensagem nova do usuário, a que está sendo enviada agora.
 * @param options         opções avançadas (temperature, max_tokens etc.).
 *                        Pode ser null/vazio.
 */
public record ChatRequest(
    String conversationId,
    List<Message> history,
    String userMessage,
    Map<String, Object> options
) {

    public ChatRequest {
        if (history == null) {
            history = List.of();
        }
        if (options == null) {
            options = Map.of();
        }
    }
}
