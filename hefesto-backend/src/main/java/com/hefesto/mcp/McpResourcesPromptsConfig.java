package com.hefesto.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefesto.agents.AgentRegistry;
import com.hefesto.llm.LlmAdapterRegistry;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Expõe Resources e Prompts MCP (além das tools). Faz do Hefesto um servidor MCP
 * "completo": clientes podem puxar contexto (agentes/modelos) e usar prompts
 * prontos que orquestram as tools, sem decorar nomes.
 */
@Configuration
public class McpResourcesPromptsConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JSON = "application/json";

    // ---------------------------------------------------------------- Resources

    @Bean
    public List<SyncResourceSpecification> hefestoResources(
            AgentRegistry agents, LlmAdapterRegistry adapters) {

        var agentsResource = new SyncResourceSpecification(
                new McpSchema.Resource("hefesto://agents", "Agentes do Hefesto",
                        "Lista dos agentes especialistas disponíveis (QA, Analista, etc.)", JSON, null),
                (exchange, request) -> {
                    List<Map<String, Object>> body = agents.list().stream()
                            .map(a -> Map.<String, Object>of(
                                    "id", a.id(),
                                    "description", a.description() == null ? "" : a.description(),
                                    "extractsTestCases", a.extractsTestCases()))
                            .toList();
                    return textResource("hefesto://agents", toJson(body));
                });

        var modelsResource = new SyncResourceSpecification(
                new McpSchema.Resource("hefesto://models", "Modelos do Hefesto",
                        "Adapters de LLM registrados e se estão disponíveis agora", JSON, null),
                (exchange, request) -> {
                    List<Map<String, Object>> body = adapters.list().stream()
                            .map(a -> Map.<String, Object>of(
                                    "id", a.id(),
                                    "displayName", a.displayName(),
                                    "available", safeAvailable(a)))
                            .toList();
                    return textResource("hefesto://models", toJson(body));
                });

        return List.of(agentsResource, modelsResource);
    }

    // ------------------------------------------------------------------ Prompts

    @Bean
    public List<SyncPromptSpecification> hefestoPrompts() {
        var revisar = new SyncPromptSpecification(
                new McpSchema.Prompt("revisar_historia",
                        "Avalia a prontidão (INVEST) de uma história do Jira",
                        List.of(new McpSchema.PromptArgument("jiraKey", "issue key, ex: HEF-8", true))),
                (exchange, request) -> {
                    String key = arg(request.arguments(), "jiraKey");
                    String text = "Use a tool review_story (model=claude-code, jiraKey=" + key
                            + ") e me apresente o score de prontidão, gaps, riscos e critérios faltantes.";
                    return userPrompt("Revisar prontidão de " + key, text);
                });

        var gerarTestes = new SyncPromptSpecification(
                new McpSchema.Prompt("gerar_testes_no_jira",
                        "Gera os casos de teste de uma história e cria as subtarefas no Jira",
                        List.of(new McpSchema.PromptArgument("jiraKey", "issue key, ex: HEF-8", true))),
                (exchange, request) -> {
                    String key = arg(request.arguments(), "jiraKey");
                    String text = "Use a tool create_test_subtasks (model=claude-code, parentKey=" + key
                            + ") pra gerar os casos de teste e criar uma subtarefa por caso sob " + key + ".";
                    return userPrompt("Gerar testes de " + key, text);
                });

        var rascunhar = new SyncPromptSpecification(
                new McpSchema.Prompt("rascunhar_historia",
                        "Gera um rascunho de história a partir de um requisito",
                        List.of(new McpSchema.PromptArgument("requisito", "texto do requisito/contexto", true))),
                (exchange, request) -> {
                    String req = arg(request.arguments(), "requisito");
                    String text = "Com base neste requisito, escreva uma história de usuário (título, "
                            + "descrição e critérios de aceite no formato Gherkin):\n\n" + req;
                    return userPrompt("Rascunhar história", text);
                });

        return List.of(revisar, gerarTestes, rascunhar);
    }

    // ------------------------------------------------------------------ helpers

    private static McpSchema.ReadResourceResult textResource(String uri, String text) {
        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(uri, JSON, text)));
    }

    private static McpSchema.GetPromptResult userPrompt(String description, String text) {
        return new McpSchema.GetPromptResult(description, List.of(
                new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(text))));
    }

    private static String arg(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null ? "" : v.toString();
    }

    private static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private static boolean safeAvailable(com.hefesto.llm.LlmAdapter a) {
        try {
            return a.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
