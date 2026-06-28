package com.hefesto.llm.local;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefesto.llm.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter para um modelo {@code .gguf} servido por um {@code llama-server}
 * (llama.cpp) com API compatível com OpenAI ({@code /v1/chat/completions}).
 *
 * <p>Cada instância aponta para UM llama-server (um modelo, uma porta). As
 * instâncias são registradas dinamicamente por {@link LocalModelsRegistrar} a
 * partir de {@code hefesto.local-models} no application.yml — não é um
 * {@code @Component} com componente-scan, é criado via BeanDefinition.</p>
 */
public class LlamaServerAdapter implements LlmAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String id;
    private final String displayName;
    private final String model;
    private final Duration timeout;
    private final WebClient webClient;

    public LlamaServerAdapter(LocalModelProperties props) {
        this.id = props.id();
        this.displayName = props.resolvedDisplayName();
        this.model = props.resolvedModel();
        this.timeout = Duration.ofSeconds(props.resolvedTimeoutSeconds());
        // WebClient próprio por modelo — buffer generoso pra respostas longas.
        this.webClient = WebClient.builder()
                .baseUrl(props.baseUrl())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean isAvailable() {
        try {
            Boolean up = webClient.get().uri("/v1/models").retrieve()
                    .toBodilessEntity().map(r -> true).block(Duration.ofSeconds(2));
            return Boolean.TRUE.equals(up);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long start = Instant.now().toEpochMilli();
        try {
            JsonNode resp = webClient.post().uri("/v1/chat/completions")
                    .bodyValue(buildPayload(request, false))
                    .retrieve().bodyToMono(JsonNode.class).block(timeout);

            String content = resp.path("choices").path(0).path("message").path("content").asText("");
            long latency = Instant.now().toEpochMilli() - start;
            return new ChatResponse(content, id, latency, model);
        } catch (Exception e) {
            throw new LlmAdapterException(
                "Falha ao chamar llama-server (" + id + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(ChatRequest request, ChatStreamHandler handler) {
        long start = Instant.now().toEpochMilli();
        StringBuilder full = new StringBuilder();

        webClient.post().uri("/v1/chat/completions")
                .bodyValue(buildPayload(request, true))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .timeout(timeout)
                .takeUntil(sse -> "[DONE]".equals(sse.data()))
                .doOnNext(sse -> {
                    if (handler.isAborted()) {
                        throw new RuntimeException("Stream abortado pelo usuário");
                    }
                    String data = sse.data();
                    if (data == null || data.isBlank() || "[DONE]".equals(data)) return;
                    String delta = extractDelta(data);
                    if (!delta.isEmpty()) {
                        full.append(delta);
                        handler.onChunk(ChatChunk.content(delta));
                    }
                })
                .doOnComplete(() -> {
                    long latency = Instant.now().toEpochMilli() - start;
                    handler.onComplete(new ChatResponse(full.toString(), id, latency, model));
                })
                .doOnError(e -> handler.onError(new LlmAdapterException(
                    "Erro no stream do llama-server (" + id + "): " + e.getMessage())))
                .subscribe();
    }

    /** Extrai {@code choices[0].delta.content} de um chunk SSE JSON do OpenAI. */
    private String extractDelta(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            return node.path("choices").path(0).path("delta").path("content").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> buildPayload(ChatRequest request, boolean stream) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (Message msg : request.history()) {
            messages.add(Map.of("role", openAiRole(msg.role()), "content", msg.content()));
        }
        messages.add(Map.of("role", "user", "content", request.userMessage()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("stream", stream);

        // Repasse de opções avançadas, se vierem.
        Map<String, Object> opts = request.options();
        if (opts != null) {
            if (opts.containsKey("temperature")) payload.put("temperature", opts.get("temperature"));
            if (opts.containsKey("max_tokens")) payload.put("max_tokens", opts.get("max_tokens"));
        }
        return payload;
    }

    /** Mapeia os papéis internos pros aceitos pela API OpenAI. */
    private static String openAiRole(Message.Role role) {
        return switch (role) {
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            // JIRA_CONTEXT e qualquer outro entram como contexto do usuário.
            default -> "user";
        };
    }
}
