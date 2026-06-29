package com.hefesto.story;

/**
 * Corpo do {@code POST /api/jira/ai/draft-story}.
 *
 * @param model   id do adapter de LLM a usar (ex: "claude-code").
 * @param context requisitos / contexto / padrões / descrição de telas em texto.
 * @param jiraKey (opcional) issue do Jira pra usar como contexto adicional.
 */
public record StoryDraftRequest(
    String model,
    String context,
    String jiraKey
) {}
