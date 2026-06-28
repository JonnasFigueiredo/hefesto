package com.hefesto.mcp;

import com.hefesto.chat.ChatService;
import com.hefesto.chat.dto.ChatRequestDto;
import com.hefesto.chat.dto.ChatResponseDto;
import com.hefesto.jira.JiraService;
import com.hefesto.jira.dto.CreatedIssueDto;
import com.hefesto.jira.dto.IssueDto;
import com.hefesto.jira.dto.IssueListDto;
import com.hefesto.testcases.TestCase;
import com.hefesto.testcases.TestCaseService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tools MCP do fluxo de QA/dev/PO: analisar requisitos, consumir e criar
 * histórias no Jira, e gerar cenários de teste. São wrappers finos sobre os
 * serviços que já existem ({@link ChatService}, {@link JiraService},
 * {@link TestCaseService}) — toda a orquestração (agente + Jira como contexto +
 * extração de casos) é reaproveitada.
 *
 * <p>As tools que dependem de LLM ({@code analyze_requirements},
 * {@code generate_test_cases}) recebem um {@code model} = id de adapter
 * (ex: "llama-3.1-8b", "claude-code"). Use {@code list_models} pra descobrir
 * quais estão disponíveis.</p>
 */
@Component
public class WorkflowMcpTools {

    /** Agentes (arquivos .md) usados por cada etapa do fluxo. */
    private static final String AGENT_ANALISTA = "AnalistaDeNegocios";
    private static final String AGENT_QA = "QAseniorAgent";

    private final ChatService chatService;
    private final JiraService jiraService;
    private final TestCaseService testCaseService;

    public WorkflowMcpTools(ChatService chatService, JiraService jiraService,
                            TestCaseService testCaseService) {
        this.chatService = chatService;
        this.jiraService = jiraService;
        this.testCaseService = testCaseService;
    }

    // ------------------------------------------------------------------ Jira (ler)

    @Tool(name = "jira_search",
          description = "Busca issues no Jira por JQL. Retorna key, título, status, tipo e link.")
    public List<IssueListDto> jiraSearch(
            @ToolParam(description = "consulta JQL, ex: 'project = PROJ AND sprint in openSprints()'") String jql,
            @ToolParam(description = "máximo de resultados (1-50)", required = false) Integer maxResults) {
        int max = (maxResults == null || maxResults <= 0) ? 20 : Math.min(maxResults, 50);
        return jiraService.search(jql, null, max).issues();
    }

    @Tool(name = "jira_get_issue",
          description = "Detalha uma issue do Jira: descrição, critérios de aceite, status, "
                      + "comentários recentes. Use antes de gerar casos de teste.")
    public IssueDto jiraGetIssue(
            @ToolParam(description = "issue key, ex: 'PROJ-123'") String key) {
        return jiraService.getIssue(key);
    }

    // ------------------------------------------------------------------ Jira (escrever)

    @Tool(name = "jira_create_story",
          description = "Cria uma história/tarefa no Jira a partir de um requisito analisado. "
                      + "Retorna a key e o link da issue criada.")
    public CreatedIssueDto jiraCreateStory(
            @ToolParam(description = "KEY do projeto, ex: 'PROJ'") String projectKey,
            @ToolParam(description = "título/summary da história") String summary,
            @ToolParam(description = "descrição em texto livre") String description,
            @ToolParam(description = "critérios de aceite, um por item", required = false) List<String> acceptanceCriteria,
            @ToolParam(description = "tipo da issue (default 'Story'); use 'Task' se o projeto não tiver Story",
                       required = false) String issueType) {
        String type = (issueType == null || issueType.isBlank()) ? "Story" : issueType;
        return jiraService.createStory(projectKey, type, summary, description, acceptanceCriteria);
    }

    @Tool(name = "jira_create_subtask",
          description = "Cria uma subtarefa pendurada numa issue pai. Útil pra registrar "
                      + "cada caso de teste ou tarefa derivada sob a história.")
    public CreatedIssueDto jiraCreateSubtask(
            @ToolParam(description = "KEY da issue pai, ex: 'PROJ-123'") String parentKey,
            @ToolParam(description = "título/summary da subtarefa") String summary,
            @ToolParam(description = "descrição em texto livre", required = false) String description,
            @ToolParam(description = "tipo da subtarefa (default 'Sub-task')", required = false) String issueType) {
        return jiraService.createSubtask(parentKey, issueType, summary, description);
    }

    @Tool(name = "jira_add_comment",
          description = "Adiciona um comentário a uma issue do Jira (ex: resumo de execução de testes).")
    public String jiraAddComment(
            @ToolParam(description = "issue key, ex: 'PROJ-123'") String key,
            @ToolParam(description = "texto do comentário") String comment) {
        jiraService.addComment(key, comment);
        return "Comentário adicionado em " + key;
    }

    @Tool(name = "jira_update_issue",
          description = "Edita uma issue existente: atualiza título e/ou descrição (e critérios "
                      + "de aceite). Campos não informados ficam intactos.")
    public String jiraUpdateIssue(
            @ToolParam(description = "issue key, ex: 'PROJ-123'") String key,
            @ToolParam(description = "novo título; deixe vazio pra não mexer", required = false) String summary,
            @ToolParam(description = "nova descrição; deixe vazio pra não mexer", required = false) String description,
            @ToolParam(description = "novos critérios de aceite (substituem a seção)", required = false) List<String> acceptanceCriteria) {
        jiraService.updateIssue(key, summary, description, acceptanceCriteria);
        return "Issue " + key + " atualizada.";
    }

    @Tool(name = "jira_transition_issue",
          description = "Move uma issue para outro status pelo nome da transição (ex: 'Em "
                      + "andamento', 'Done'). Se o nome não existir, retorna as opções válidas.")
    public String jiraTransitionIssue(
            @ToolParam(description = "issue key, ex: 'PROJ-123'") String key,
            @ToolParam(description = "nome da transição/status destino, ex: 'Done'") String transition) {
        jiraService.transitionIssue(key, transition);
        return "Issue " + key + " movida para '" + transition + "'.";
    }

    // ------------------------------------------------------------------ LLM (analisar / gerar)

    @Tool(name = "analyze_requirements",
          description = "Analisa um requisito com o agente Analista de Negócios: aponta gaps, "
                      + "ambiguidades, riscos e sugere critérios de aceite. Aceita texto livre "
                      + "e/ou uma issue do Jira como contexto.")
    public String analyzeRequirements(
            @ToolParam(description = "id do modelo, ex: 'claude-code' ou 'llama-3.1-8b'") String model,
            @ToolParam(description = "texto do requisito a analisar", required = false) String requirementText,
            @ToolParam(description = "issue key do Jira pra usar como contexto, ex: 'PROJ-123'",
                       required = false) String jiraKey) {
        String message = (requirementText == null || requirementText.isBlank())
                ? "Analise o requisito anexado e aponte gaps, ambiguidades, riscos e critérios de aceite."
                : requirementText;
        ChatResponseDto resp = chatService.sendMessage(new ChatRequestDto(
                model, null, message, AGENT_ANALISTA, null, blankToNull(jiraKey)));
        return resp.content();
    }

    /** Resultado de {@link #generateTestCases}. */
    public record GeneratedTestCases(String narrative, List<TestCase> testCases) {}

    @Tool(name = "generate_test_cases",
          description = "Gera cenários de teste estruturados (formato TC-NNN) com o agente QA "
                      + "Sênior, com base num contexto e/ou numa issue do Jira. Retorna a "
                      + "resposta e a lista de casos extraídos e persistidos.")
    public GeneratedTestCases generateTestCases(
            @ToolParam(description = "id do modelo, ex: 'claude-code' ou 'llama-3.1-8b'") String model,
            @ToolParam(description = "contexto/instrução adicional pra geração", required = false) String context,
            @ToolParam(description = "issue key do Jira pra usar como base, ex: 'PROJ-123'",
                       required = false) String jiraKey) {
        String message = (context == null || context.isBlank())
                ? "Gere casos de teste cobrindo a funcionalidade descrita (positivos, negativos e edge)."
                : context;
        ChatResponseDto resp = chatService.sendMessage(new ChatRequestDto(
                model, null, message, AGENT_QA, null, blankToNull(jiraKey)));
        List<TestCase> extracted = testCaseService.findByConversation(resp.conversationId());
        return new GeneratedTestCases(resp.content(), extracted);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
