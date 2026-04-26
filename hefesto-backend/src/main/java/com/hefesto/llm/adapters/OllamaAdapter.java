package com.hefesto.llm.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.hefesto.llm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter simplificado para consumir a API REST local do Ollama.
 */
@Component
public class OllamaAdapter implements LlmAdapter {

    private final WebClient webClient;
    private final String model;
    private final Duration timeout;

    public OllamaAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${ollama.api.url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:qwen2.5:3b}") String model,
            @Value("${ollama.timeoutSeconds:60}") long timeoutSeconds
    ) {
        // Correção principal: Utilizando o WebClient correto do WebFlux
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public String id() {
        return "ollama-local";
    }

    @Override
    public String displayName() {
        return "Ollama (" + model + ")";
    }

    @Override
    public boolean isAvailable() {
        try {
            // Ping rápido para verificar se a API está online
            Boolean isUp = webClient.get().uri("/api/tags").retrieve()
                    .toBodilessEntity().map(resp -> true).block(Duration.ofSeconds(2));
            return Boolean.TRUE.equals(isUp);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long start = Instant.now().toEpochMilli();
        try {
            JsonNode response = webClient.post().uri("/api/chat")
                    .bodyValue(buildPayload(request, false))
                    .retrieve().bodyToMono(JsonNode.class).block(timeout);

            String content = response.path("message").path("content").asText();
            long latency = Instant.now().toEpochMilli() - start;
            
            return new ChatResponse(content, id(), latency, model);
        } catch (Exception e) {
            throw new LlmAdapterException("Falha na comunicação com Ollama: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(ChatRequest request, ChatStreamHandler handler) {
        long start = Instant.now().toEpochMilli();
        StringBuilder fullText = new StringBuilder();

        webClient.post().uri("/api/chat")
                .bodyValue(buildPayload(request, true))
                .retrieve().bodyToFlux(JsonNode.class).timeout(timeout)
                .doOnNext(node -> {
                    if (handler.isAborted()) {
                        throw new RuntimeException("Stream abortado pelo usuário");
                    }
                    String chunk = node.path("message").path("content").asText("");
                    if (!chunk.isEmpty()) {
                        fullText.append(chunk);
                        handler.onChunk(ChatChunk.content(chunk));
                    }
                })
                .doOnComplete(() -> {
                    long latency = Instant.now().toEpochMilli() - start;
                    handler.onComplete(new ChatResponse(fullText.toString(), id(), latency, model));
                })
                .doOnError(e -> handler.onError(new LlmAdapterException("Erro no stream: " + e.getMessage())))
                .subscribe();
    }

    private Map<String, Object> buildPayload(ChatRequest request, boolean stream) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // Mapeia o histórico usando foreach para simplificar
        request.history().forEach(msg -> 
            messages.add(Map.of("role", msg.role().name().toLowerCase(), "content", msg.content()))
        );
        
        messages.add(Map.of("role", "user", "content", request.userMessage()));

        return Map.of("model", model, "messages", messages, "stream", stream);
    }
}