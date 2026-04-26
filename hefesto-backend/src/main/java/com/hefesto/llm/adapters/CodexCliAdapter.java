package com.hefesto.llm.adapters;

import org.springframework.stereotype.Component;

import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;

/**
 * Stub para o CLI do Codex (OpenAI). Reservado para implementação futura.
 */
@Component
public class CodexCliAdapter implements LlmAdapter {

    @Override public String id() { return "codex-cli"; }
    @Override public String displayName() { return "Codex CLI"; }
    @Override public boolean isAvailable() { return false; }

    @Override
    public ChatResponse chat(ChatRequest request) {
        throw new LlmAdapterException("Codex CLI ainda não implementado");
    }
}
