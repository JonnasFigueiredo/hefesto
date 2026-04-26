package com.hefesto.llm;

/**
 * Callback handler usado pelo {@link LlmAdapter#chatStream} pra entregar
 * chunks à medida que o LLM produz texto.
 *
 * <p>Implementações típicas: WebSocket que reenvia cada chunk pro cliente,
 * coletor in-memory pra testes, etc.</p>
 */
public interface ChatStreamHandler {

    /**
     * Chamado pra cada chunk de conteúdo (texto incremental) ou metadata.
     * Não chamado pra DONE/ERROR — esses vão pelos métodos específicos.
     */
    void onChunk(ChatChunk chunk);

    /**
     * Chamado uma vez quando o stream termina com sucesso. {@code response}
     * agrega o conteúdo total e meta-informações.
     */
    void onComplete(ChatResponse response);

    /**
     * Chamado uma vez se algo der errado (binary missing, exit não-zero,
     * I/O exception etc.). Após esta chamada, nenhum outro callback dispara.
     */
    void onError(Throwable error);

    /**
     * Sinaliza ao adapter se o cliente quer abortar o streaming. Adapters
     * devem checar periodicamente e parar (matar subprocess) se for true.
     */
    default boolean isAborted() {
        return false;
    }
}
