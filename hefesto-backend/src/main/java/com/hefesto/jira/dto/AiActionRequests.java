package com.hefesto.jira.dto;

/**
 * Corpos das ações de IA disparadas pelo painel do Jira (REST), que delegam
 * para o componente de workflow (as mesmas operações expostas via MCP).
 */
public final class AiActionRequests {

    private AiActionRequests() {}

    /** {@code POST /api/jira/ai/review-story}. */
    public record ReviewStoryRequest(String model, String jiraKey, Boolean postComment) {}

    /** {@code POST /api/jira/ai/test-subtasks}. */
    public record TestSubtasksRequest(String model, String parentKey, String context) {}
}
