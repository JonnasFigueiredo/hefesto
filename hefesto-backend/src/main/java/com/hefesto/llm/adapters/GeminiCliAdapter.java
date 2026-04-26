package com.hefesto.llm.adapters;

import org.springframework.stereotype.Component;

import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;

/**
 * Stub para o CLI do Gemini. Reservado para implementação futura.
 * Sempre {@code isAvailable() = false} até ser implementado.
 */
@Component
public class GeminiCliAdapter implements LlmAdapter {

    @Override public String id() { return "gemini-cli"; }
    @Override public String displayName() { return "Gemini CLI"; }
    @Override public boolean isAvailable() { return false; }

    @Override
    public ChatResponse chat(ChatRequest request) {
        throw new LlmAdapterException("Gemini CLI ainda não implementado");
    }
}
