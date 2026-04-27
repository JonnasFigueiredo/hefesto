package com.hefesto.llm.adapters;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.hefesto.llm.ChatRequest;
import com.hefesto.llm.ChatResponse;
import com.hefesto.llm.LlmAdapter;
import com.hefesto.llm.LlmAdapterException;

/**
 * Adapter que conversa com a extensão VS Code "Hefesto Bridge", que expõe
 * os modelos do GitHub Copilot via HTTP local.
 *
 * <p>Vantagens de usar via bridge:</p>
 * <ul>
 *   <li>Aproveita a license Copilot já paga pela empresa.</li>
 *   <li>Mesma arquitetura de adapter — nada muda em prompt builder, agentes
 *       especialistas, attachments, Jira context, etc.</li>
 *   <li>Não viola ToS — usa a API oficial vscode.lm.</li>
 * </ul>
 *
 * <p>Pré-requisitos pra ficar disponível:</p>
 * <ol>
 *   <li>Usuário tem VS Code aberto;</li>
 *   <li>Extensão "Hefesto Bridge" instalada e habilitada;</li>
 *   <li>GitHub Copilot ativo na conta logada no VS Code.</li>
 * </ol>
 */
@Component
public class CopilotBridgeAdapter implements LlmAdapter {

    private static final Logger log = LoggerFactory.getLogger(CopilotBridgeAdapter.class);

    private final String bridgeUrl;
    private final String preferredFamily;
    private final RestClient httpClient;

    public CopilotBridgeAdapter(
        @Value("${copilot.bridge.url:http://localhost:35421}") String bridgeUrl,
        @Value("${copilot.bridge.preferredFamily:}") String preferredFamily,
        @Value("${copilot.bridge.timeoutSeconds:120}") long timeoutSeconds
    ) {
        this.bridgeUrl = stripTrailingSlash(bridgeUrl);
        this.preferredFamily = preferredFamily;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
        this.httpClient = RestClient.builder()
            .requestFactory((ClientHttpRequestFactory) factory)
            .build();
    }

    @Override
    public String id() {
        return "copilot-bridge";
    }

    @Override
    public String displayName() {
        return "GitHub Copilot";
    }

    @Override
    public boolean isAvailable() {
        try {
            JsonNode health = httpClient.get()
                .uri(bridgeUrl + "/health")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

            if (health == null) return false;
            boolean ok = "ok".equals(health.path("status").asText(""));
            int models = health.path("modelCount").asInt(0);
            if (!ok) {
                log.warn("Copilot bridge respondeu mas status != ok: {}", health);
                return false;
            }
            if (models == 0) {
                log.warn("Copilot bridge OK mas modelCount=0 (Copilot não logado no VS Code?)");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.debug("Copilot bridge indisponível em {}: {}", bridgeUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", request.userMessage() == null ? "" : request.userMessage());
        if (preferredFamily != null && !preferredFamily.isBlank()) {
            body.put("family", preferredFamily);
        }

        log.debug("POST {}/chat (prompt {} chars, family={})",
            bridgeUrl,
            request.userMessage() == null ? 0 : request.userMessage().length(),
            preferredFamily.isBlank() ? "<auto>" : preferredFamily);

        JsonNode response;
        try {
            response = httpClient.post()
                .uri(bridgeUrl + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        } catch (Exception e) {
            // Diferencia "extensão não está rodando" de "extensão respondeu erro".
            if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
                throw new LlmAdapterException(
                    "Extensão Hefesto Bridge não está rodando. Abra o VS Code com a extensão instalada.",
                    e);
            }
            throw new LlmAdapterException(
                "Falha ao chamar Copilot Bridge: " + e.getMessage(), e);
        }

        if (response == null) {
            throw new LlmAdapterException("Copilot Bridge retornou body vazio");
        }

        if (response.has("error")) {
            String code = response.path("error").asText("");
            String msg = response.path("message").asText(code);
            throw new LlmAdapterException("Copilot Bridge erro: " + msg);
        }

        String content = response.path("content").asText("");
        if (content.isBlank()) {
            throw new LlmAdapterException("Copilot retornou conteúdo vazio");
        }

        String model = response.path("model").asText(null);
        long latency = response.path("latencyMs").asLong(0);

        return new ChatResponse(content, id(), latency, model);
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
