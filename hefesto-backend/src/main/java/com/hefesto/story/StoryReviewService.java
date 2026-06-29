package com.hefesto.story;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefesto.chat.ChatService;
import com.hefesto.chat.dto.ChatRequestDto;
import com.hefesto.chat.dto.ChatResponseDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Avalia a prontidão (INVEST) de uma história do Jira rodando o agente
 * {@code RevisorDeHistorias} via {@link ChatService} (com a issue como contexto)
 * e parseando o JSON de saída em {@link StoryReview}.
 */
@Service
public class StoryReviewService {

    static final String AGENT = "RevisorDeHistorias";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatService chatService;

    public StoryReviewService(ChatService chatService) {
        this.chatService = chatService;
    }

    public StoryReview review(String model, String jiraKey, String context) {
        String message = (context == null || context.isBlank())
                ? "Avalie a prontidão da história (INVEST) e aponte gaps, riscos e critérios faltantes."
                : context;
        String key = (jiraKey == null || jiraKey.isBlank()) ? null : jiraKey;

        ChatResponseDto resp = chatService.sendMessage(
                new ChatRequestDto(model, null, message, AGENT, null, key));
        return parse(resp.content());
    }

    static StoryReview parse(String content) {
        // Reusa a extração de JSON robusta do StoryDraftService.
        String json = StoryDraftService.extractJson(content);
        try {
            JsonNode n = MAPPER.readTree(json);
            if (n.isObject()) {
                int score = clampScore(n.path("readinessScore").asInt(0));
                String verdict = n.path("verdict").asText("").strip();
                return new StoryReview(
                        score,
                        verdict,
                        toList(n.path("invest")),
                        toList(n.path("gaps")),
                        toList(n.path("risks")),
                        toList(n.path("missingCriteria")),
                        content);
            }
        } catch (Exception ignore) {
            // não era JSON — cai no fallback.
        }
        return new StoryReview(0, "Não foi possível estruturar a revisão.",
                List.of(), List.of(), List.of(), List.of(), content == null ? "" : content.strip());
    }

    private static int clampScore(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static List<String> toList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr.isArray()) {
            arr.forEach(x -> {
                String v = x.asText("").strip();
                if (!v.isEmpty()) out.add(v);
            });
        }
        return out;
    }
}
