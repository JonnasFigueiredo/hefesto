package com.hefesto.agents;

/**
 * DTO de resposta — não expõe o systemPrompt completo (poderia vazar
 * estratégia/limites). Cliente vê só metadados + filename.
 */
public record AgentDto(
    String id,
    String name,
    String description,
    String defaultPromptTemplate,
    String emoji,
    boolean extractsTestCases
) {

    public static AgentDto from(Agent agent) {
        return new AgentDto(
            agent.id(),
            agent.name(),
            agent.description(),
            agent.defaultPromptTemplate(),
            agent.emoji(),
            agent.extractsTestCases()
        );
    }
}
