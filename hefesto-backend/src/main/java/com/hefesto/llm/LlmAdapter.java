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

    /**
     * Versão streaming da chamada. Cada chunk é entregue ao {@code handler}
     * conforme o LLM produz texto. Bloqueia a thread chamadora até completar
     * ou abortar (use uma thread dedicada se quiser concorrência).
     *
     * <p>Implementação default: chama {@link #chat(ChatRequest)} e emite a
     * resposta completa como um único chunk. Adapters que suportam streaming
     * real devem sobrescrever (ex: ClaudeCodeCliAdapter usa
     * {@code --output-format stream-json}).</p>
     */
    default void chatStream(ChatRequest request, ChatStreamHandler handler) {
        try {
            ChatResponse response = chat(request);
            if (handler.isAborted()) return;
            if (response.content() != null && !response.content().isEmpty()) {
                handler.onChunk(ChatChunk.content(response.content()));
            }
            handler.onComplete(response);
        } catch (Throwable t) {
            handler.onError(t);
        }
    }
}
