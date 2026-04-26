package com.hefesto.llm.adapters;

import org.springframework.stereotype.Component;

import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;

/**
 * Stub para chamada direta na API REST da Anthropic (sem CLI).
 * Reservado para implementação futura.
 */
@Component
public class AnthropicApiAdapter implements LlmAdapter {

    @Override public String id() { return "anthropic-api"; }
    @Override public String displayName() { return "Anthropic API"; }
    @Override public boolean isAvailable() { return false; }

    @Override
    public ChatResponse chat(ChatRequest request) {
        throw new LlmAdapterException("Anthropic API direta ainda não implementada");
    }
}
