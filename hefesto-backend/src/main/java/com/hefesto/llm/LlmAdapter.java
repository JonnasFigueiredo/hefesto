package com.hefesto.llm;

/**
 * Contrato central de integração com qualquer LLM. Toda implementação concreta
 * (CLI ou API direta) precisa implementar esta interface e ser registrada como
 * bean Spring para o {@link LlmAdapterRegistry} encontrá-la automaticamente.
 *
 * <p>Etapa 2: chat síncrono. Etapa 3: refatoração para streaming (Flux&lt;ChatChunk&gt;).</p>
 */
public interface LlmAdapter {

    /**
     * Identificador estável usado em URLs e na seleção feita pelo frontend.
     * Convenção: kebab-case. Ex: "claude-code", "gemini-cli", "anthropic-api".
     */
    String id();

    /**
     * Nome legível mostrado no dropdown do frontend. Ex: "Claude Code".
     */
    String displayName();

    /**
     * Indica se o adapter está disponível agora (binário instalado, API
     * respondendo, credenciais válidas). Chamado pelo frontend para desabilitar
     * opções indisponíveis no dropdown.
     */
    boolean isAvailable();

    /**
     * Executa uma chamada de chat síncrona. Bloqueia até a resposta completa.
     *
     * @throws LlmAdapterException em caso de falha (binário ausente, exit code
     *         não-zero, timeout etc.). Mensagens devem ser claras e seguras
     *         para mostrar ao usuário (sem expor caminhos de credenciais).
     */
    ChatResponse chat(ChatRequest request);
}
