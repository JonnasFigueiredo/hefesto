package com.hefesto.agents;

/**
 * Agente especialista — uma persona com system prompt e template padrão de
 * uso, selecionável pelo usuário antes de enviar uma mensagem.
 *
 * <p>Carregado em runtime a partir de arquivos .md na pasta de agentes
 * (configurável via {@code agents.path}). O nome exibido na UI é o nome
 * completo do arquivo (ex: "QAseniorAgent.md").</p>
 *
 * @param id                     identificador estável (filename sem extensão).
 * @param name                   nome legível mostrado na UI (filename completo).
 * @param description            1-2 linhas resumindo o que o agente faz.
 * @param systemPrompt           prompt-base injetado antes de qualquer mensagem.
 * @param defaultPromptTemplate  sugestão de mensagem inicial; pode ser null.
 * @param emoji                  ícone curto pra UI; pode ser null.
 * @param extractsTestCases      se true, o backend roda o extractor sobre
 *                               respostas deste agente buscando casos de
 *                               teste estruturados.
 */
public record Agent(
    String id,
    String name,
    String description,
    String systemPrompt,
    String defaultPromptTemplate,
    String emoji,
    boolean extractsTestCases
) {

    public static final String DEFAULT_ID = "Default";

    public boolean hasSystemPrompt() {
        return systemPrompt != null && !systemPrompt.isBlank();
    }
}
