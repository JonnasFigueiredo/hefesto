package com.hefesto.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registro central de adapters. O Spring injeta automaticamente todos os beans
 * que implementam {@link LlmAdapter}; o registry indexa por id e expõe lookup
 * para o ChatService e listagem para o frontend.
 */
@Component
public class LlmAdapterRegistry {

    private static final Logger log = LoggerFactory.getLogger(LlmAdapterRegistry.class);

    private final Map<String, LlmAdapter> byId;

    public LlmAdapterRegistry(List<LlmAdapter> adapters) {
        // LinkedHashMap preserva ordem de injeção, que respeita @Order ou
        // ordem alfabética de classe — assim o Claude Code aparece primeiro.
        this.byId = new LinkedHashMap<>();
        for (LlmAdapter adapter : adapters) {
            this.byId.put(adapter.id(), adapter);
        }
        log.info("Registered {} LLM adapter(s): {}", byId.size(), byId.keySet());
    }

    /**
     * Lista todos os adapters registrados em ordem estável.
     */
    public List<LlmAdapter> list() {
        return List.copyOf(byId.values());
    }

    /**
     * Resolve um adapter por id. Retorna empty se não existir.
     */
    public Optional<LlmAdapter> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
