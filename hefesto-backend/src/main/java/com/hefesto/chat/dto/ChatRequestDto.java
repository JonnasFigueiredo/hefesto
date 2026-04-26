package com.hefesto.chat.dto;

/**
 * DTO de entrada do {@code POST /api/chat}.
 *
 * @param adapterId       qual LLM usar (ex: "claude-code").
 * @param conversationId  id da conversa existente; null/blank cria nova.
 * @param message         mensagem nova do usuário.
 */
public record ChatRequestDto(
    String adapterId,
    String conversationId,
    String message
) {}
