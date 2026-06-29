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
 * Gera um {@link StoryDraft} a partir de requisitos/contexto, rodando o agente
 * {@code EscritorDeHistorias} via {@link ChatService} (reusa toda a orquestração:
 * modelo + agente + issue do Jira como contexto) e parseando o JSON de saída.
 */
@Service
public class StoryDraftService {

    static final String AGENT = "EscritorDeHistorias";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatService chatService;

    public StoryDraftService(ChatService chatService) {
        this.chatService = chatService;
    }

    public StoryDraft draft(StoryDraftRequest req) {
        String message = (req.context() == null || req.context().isBlank())
                ? "Escreva a história com base no contexto fornecido."
                : req.context();
        String jiraKey = (req.jiraKey() == null || req.jiraKey().isBlank()) ? null : req.jiraKey();

        ChatResponseDto resp = chatService.sendMessage(
                new ChatRequestDto(req.model(), null, message, AGENT, null, jiraKey));
        return parse(resp.content());
    }

    /**
     * Parseia a resposta do modelo num {@link StoryDraft}. Robusto a JSON dentro
     * de cercas ```json, texto ao redor, ou ausência de JSON (cai em fallback que
     * joga a resposta inteira na descrição, pra nunca perder o trabalho do modelo).
     */
    static StoryDraft parse(String content) {
        String json = extractJson(content);
        try {
            JsonNode n = MAPPER.readTree(json);
            if (n.isObject()) {
                String summary = n.path("summary").asText("").strip();
                String description = n.path("description").asText("").strip();
                List<String> criteria = new ArrayList<>();
                JsonNode ac = n.path("acceptanceCriteria");
                if (ac.isArray()) {
                    ac.forEach(c -> {
                        String v = c.asText("").strip();
                        if (!v.isEmpty()) criteria.add(v);
                    });
                }
                if (!summary.isEmpty() || !description.isEmpty() || !criteria.isEmpty()) {
                    return new StoryDraft(summary, description, criteria, content);
                }
            }
        } catch (Exception ignore) {
            // não era JSON válido — cai no fallback.
        }
        return new StoryDraft("", content == null ? "" : content.strip(), List.of(), content);
    }

    /** Extrai o objeto JSON da resposta: cerca ```json, ``` ou do 1º { ao último }. */
    static String extractJson(String s) {
        if (s == null) return "";
        String t = s.strip();

        int fence = t.indexOf("```json");
        if (fence >= 0) {
            int start = t.indexOf('\n', fence);
            int end = t.indexOf("```", fence + 7);
            if (start >= 0 && end > start) return t.substring(start + 1, end).strip();
        }
        // qualquer cerca ```
        int anyFence = t.indexOf("```");
        if (anyFence >= 0) {
            int start = t.indexOf('\n', anyFence);
            int end = t.indexOf("```", anyFence + 3);
            if (start >= 0 && end > start) {
                String inner = t.substring(start + 1, end).strip();
                if (inner.startsWith("{")) return inner;
            }
        }
        int first = t.indexOf('{');
        int last = t.lastIndexOf('}');
        if (first >= 0 && last > first) return t.substring(first, last + 1);
        return t;
    }
}
