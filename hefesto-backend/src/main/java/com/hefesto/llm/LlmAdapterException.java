package com.hefesto.llm;

/**
 * Exceção lançada por adapters quando uma chamada de chat falha.
 * O ChatController converte em HTTP 502/503 com a mensagem.
 */
public class LlmAdapterException extends RuntimeException {

    public LlmAdapterException(String message) {
        super(message);
    }

    public LlmAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
