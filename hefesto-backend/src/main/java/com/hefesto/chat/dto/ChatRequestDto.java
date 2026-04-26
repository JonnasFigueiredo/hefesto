package com.hefesto.chat.dto;

import java.util.List;

/**
 * DTO de entrada do {@code POST /api/chat}.
 *
 * @param adapterId       qual LLM usar (ex: "claude-code").
 * @param conversationId  id da conversa existente; null/blank cria nova.
 * @param message         mensagem nova do usuário (texto bruto).
 * @param agentId         id do agente especialista (ex: "qa-specialist").
 *                        Se null/blank, usa "default" (sem persona).
 * @param attachmentIds   ids dos anexos a injetar como contexto. Pode ser null.
 * @param jiraIssueKey    KEY da issue do Jira a injetar como contexto.
 *                        Pode ser null/blank.
 */
public record ChatRequestDto(
    String adapterId,
    String conversationId,
    String message,
    String agentId,
    List<String> attachmentIds,
    String jiraIssueKey
) {}
