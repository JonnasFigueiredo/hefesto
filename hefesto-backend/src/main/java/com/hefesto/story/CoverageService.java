package com.hefesto.story;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefesto.chat.ChatService;
import com.hefesto.chat.dto.ChatRequestDto;
import com.hefesto.chat.dto.ChatResponseDto;
import com.hefesto.jira.JiraService;
import com.hefesto.jira.dto.IssueListDto;
import com.hefesto.story.CoverageReport.CriterionCoverage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera a matriz de cobertura cruzando os critérios de aceite de uma história
 * (que chegam como contexto Jira) com os casos de teste existentes (as
 * subtarefas da história, passadas na mensagem). Roda o agente
 * {@code AnalistaDeCobertura} via {@link ChatService} e parseia o JSON.
 */
@Service
public class CoverageService {

    static final String AGENT = "AnalistaDeCobertura";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatService chatService;
    private final JiraService jiraService;

    public CoverageService(ChatService chatService, JiraService jiraService) {
        this.chatService = chatService;
        this.jiraService = jiraService;
    }

    public CoverageReport report(String model, String jiraKey) {
        List<IssueListDto> subs = jiraService.getSubtasks(jiraKey);

        StringBuilder msg = new StringBuilder("Casos de teste existentes (subtarefas da história):\n");
        if (subs.isEmpty()) {
            msg.append("(nenhum caso de teste encontrado como subtarefa)\n");
        } else {
            for (IssueListDto s : subs) {
                msg.append("- ").append(s.key()).append(": ").append(s.summary()).append('\n');
            }
        }
        msg.append("\nCruze cada critério de aceite da história com esses casos e ")
           .append("produza a matriz de cobertura no formato JSON especificado.");

        ChatResponseDto resp = chatService.sendMessage(
                new ChatRequestDto(model, null, msg.toString(), AGENT, null, jiraKey));
        return parse(resp.content());
    }

    static CoverageReport parse(String content) {
        String json = StoryDraftService.extractJson(content);
        try {
            JsonNode n = MAPPER.readTree(json);
            if (n.isObject()) {
                int pct = Math.max(0, Math.min(100, n.path("coveragePercent").asInt(0)));
                List<CriterionCoverage> criteria = new ArrayList<>();
                JsonNode arr = n.path("criteria");
                if (arr.isArray()) {
                    for (JsonNode c : arr) {
                        String criterion = c.path("criterion").asText("").strip();
                        if (criterion.isEmpty()) continue;
                        boolean covered = c.path("covered").asBoolean(false);
                        List<String> byTests = new ArrayList<>();
                        c.path("byTests").forEach(t -> {
                            String v = t.asText("").strip();
                            if (!v.isEmpty()) byTests.add(v);
                        });
                        criteria.add(new CriterionCoverage(criterion, covered, byTests));
                    }
                }
                if (!criteria.isEmpty()) {
                    return new CoverageReport(pct, criteria, content);
                }
            }
        } catch (Exception ignore) {
            // não era JSON — fallback.
        }
        return new CoverageReport(0, List.of(), content == null ? "" : content.strip());
    }
}
