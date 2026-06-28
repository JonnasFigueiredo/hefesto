package com.hefesto.llm.local;

/**
 * Descreve um modelo local servido por um {@code llama-server} (llama.cpp) com
 * API compatível com OpenAI. Cada entrada de {@code hefesto.local-models} no
 * application.yml vira um {@link LlamaServerAdapter} próprio — selecionável no
 * frontend e exposto como destino da tool MCP {@code chat}.
 *
 * @param id             identificador estável (kebab-case). Ex: "llama-3.1-8b".
 * @param displayName    nome legível no dropdown. Se null, usa o id.
 * @param baseUrl        URL base do llama-server. Ex: "http://localhost:8081".
 * @param model          nome do modelo reportado no payload OpenAI. Se null, usa o id.
 * @param timeoutSeconds timeout da chamada. Se null, usa 120s.
 */
public record LocalModelProperties(
    String id,
    String displayName,
    String baseUrl,
    String model,
    Integer timeoutSeconds
) {
    public String resolvedDisplayName() {
        return (displayName == null || displayName.isBlank()) ? id : displayName;
    }

    public String resolvedModel() {
        return (model == null || model.isBlank()) ? id : model;
    }

    public long resolvedTimeoutSeconds() {
        return timeoutSeconds == null ? 120L : timeoutSeconds;
    }
}
