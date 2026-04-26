package com.hefesto.agents;

/**
 * Agente especialista — uma persona com system prompt e template padrão de
 * uso, selecionável pelo usuário antes de enviar uma mensagem.
 *
 * @param id                     identificador estável (kebab-case).
 * @param name                   nome legível mostrado na UI.
 * @param description            1-2 linhas resumindo o que o agente faz.
 * @param systemPrompt           prompt-base injetado antes de qualquer mensagem.
 * @param defaultPromptTemplate  sugestão de mensagem inicial; pode ser null.
 * @param emoji                  ícone curto pra UI; pode ser null.
 */
public record Agent(
    String id,
    String name,
    String description,
    String systemPrompt,
    String defaultPromptTemplate,
    String emoji
) {

    public static final String DEFAULT_ID = "default";

    public boolean hasSystemPrompt() {
        return systemPrompt != null && !systemPrompt.isBlank();
    }
}
