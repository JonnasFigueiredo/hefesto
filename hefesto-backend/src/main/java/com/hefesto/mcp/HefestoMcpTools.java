package com.hefesto.mcp;

import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterRegistry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tools MCP expostas pelo Hefesto. Cada método {@code @Tool} vira uma ferramenta
 * descobrível por qualquer cliente MCP (Claude Code, Claude Desktop, etc.),
 * roteando para os adapters de LLM já registrados — incluindo os modelos locais
 * (.gguf via llama-server) e os adapters CLI/API existentes.
 */
@Component
public class HefestoMcpTools {

    private final LlmAdapterRegistry registry;

    public HefestoMcpTools(LlmAdapterRegistry registry) {
        this.registry = registry;
    }

    /** Item retornado por {@link #listModels()}. */
    public record ModelInfo(String id, String displayName, boolean available) {}

    @Tool(name = "list_models",
          description = "Lista todos os modelos/adapters de LLM disponíveis no Hefesto "
                      + "(modelos locais .gguf via llama-server, Claude Code, Gemini, etc.), "
                      + "com seu id, nome legível e se estão disponíveis agora.")
    public List<ModelInfo> listModels() {
        return registry.list().stream()
                .map(a -> new ModelInfo(a.id(), a.displayName(), safeAvailable(a)))
                .toList();
    }

    @Tool(name = "chat",
          description = "Envia um prompt a um modelo específico do Hefesto e retorna a "
                      + "resposta completa. Use 'list_models' antes para descobrir os ids "
                      + "válidos (ex: 'llama-3.1-8b', 'mistral-7b', 'qwen3-8b', 'claude-code').")
    public String chat(
            @ToolParam(description = "id do modelo, ex: 'llama-3.1-8b'") String model,
            @ToolParam(description = "mensagem/prompt do usuário") String message) {

        LlmAdapter adapter = registry.get(model).orElseThrow(() ->
                new IllegalArgumentException(
                        "Modelo desconhecido: '" + model + "'. Use 'list_models' para ver os ids válidos."));

        ChatResponse response = adapter.chat(
                new ChatRequest(null, List.of(), message, Map.of()));
        return response.content();
    }

    private static boolean safeAvailable(LlmAdapter adapter) {
        try {
            return adapter.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
