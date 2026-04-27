package com.hefesto.jira;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cliente HTTP para a Jira Cloud REST API v3. Faz auth básica com email +
 * API token (formato padrão Atlassian Cloud).
 *
 * <p>Não trata transformação para DTOs — retorna o JsonNode cru. Conversão
 * fica no {@link JiraService}.</p>
 */
@Component
public class JiraClient {

    private static final Logger log = LoggerFactory.getLogger(JiraClient.class);

    private final JiraProperties props;
    private final RestClient httpClient;

    public JiraClient(JiraProperties props) {
        this.props = props;
        this.httpClient = RestClient.builder().build();
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /**
     * Busca issues por JQL.
     *
     * <p>Usa o endpoint <b>novo</b> {@code POST /rest/api/3/search/jql}, que
     * substituiu o legado {@code GET /rest/api/3/search} (descontinuado pela
     * Atlassian em 2024-2025). Diferenças relevantes:</p>
     *
     * <ul>
     *   <li>Verbo: POST com body JSON (não mais query params).</li>
     *   <li>Paginação: {@code nextPageToken} + {@code isLast},
     *       não mais {@code startAt}/{@code total}.</li>
     *   <li>Resposta não traz {@code total} (precisa endpoint separado
     *       {@code /search/approximate-count}).</li>
     * </ul>
     *
     * @param nextPageToken token retornado pela página anterior; null para
     *                      primeira página.
     */
    public JsonNode searchIssues(String jql, String nextPageToken, int maxResults) {
        ensureConfigured();
        String uri = UriComponentsBuilder.fromHttpUrl(props.url())
            .path("/rest/api/3/search/jql")
            .build()
            .toUriString();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jql", jql == null ? "" : jql);
        body.put("fields", List.of(
            "summary", "status", "issuetype", "priority",
            "assignee", "reporter", "updated"
        ));
        body.put("maxResults", maxResults);
        if (nextPageToken != null && !nextPageToken.isBlank()) {
            body.put("nextPageToken", nextPageToken);
        }

        return post(uri, body);
    }

    /** Detalhe completo da issue. */
    public JsonNode getIssue(String key) {
        ensureConfigured();
        String uri = UriComponentsBuilder.fromHttpUrl(props.url())
            .path("/rest/api/3/issue/{key}")
            .queryParam("expand", "renderedFields")
            .buildAndExpand(key)
            .toUriString();
        return get(uri);
    }

    /** Lista de comentários da issue. */
    public JsonNode getComments(String key) {
        ensureConfigured();
        String uri = UriComponentsBuilder.fromHttpUrl(props.url())
            .path("/rest/api/3/issue/{key}/comment")
            .queryParam("orderBy", "created")
            .buildAndExpand(key)
            .toUriString();
        return get(uri);
    }

    /** Identidade do usuário autenticado — bom pra teste de credenciais. */
    public JsonNode getCurrentUser() {
        ensureConfigured();
        String uri = UriComponentsBuilder.fromHttpUrl(props.url())
            .path("/rest/api/3/myself")
            .toUriString();
        return get(uri);
    }

    private JsonNode post(String uri, Map<String, Object> body) {
        log.debug("POST {} body={}", uri, body);
        try {
            return httpClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            log.warn("Jira API client error: {} on {}", status, uri);
            throw new JiraApiException(
                status.value(),
                "Jira respondeu " + status + ": " + extractMessage(e.getResponseBodyAsString()),
                e
            );
        } catch (HttpServerErrorException e) {
            log.warn("Jira API server error: {} on {}", e.getStatusCode(), uri);
            throw new JiraApiException(
                e.getStatusCode().value(),
                "Jira indisponível: " + e.getStatusCode(),
                e
            );
        } catch (Exception e) {
            log.warn("Jira POST failed for {}: {}", uri, e.getMessage());
            throw new JiraApiException(0, "Erro de rede chamando Jira: " + e.getMessage(), e);
        }
    }

    private JsonNode get(String uri) {
        log.debug("GET {}", uri);
        try {
            return httpClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            log.warn("Jira API client error: {} on {}", status, uri);
            throw new JiraApiException(
                status.value(),
                "Jira respondeu " + status + ": " + extractMessage(e.getResponseBodyAsString()),
                e
            );
        } catch (HttpServerErrorException e) {
            log.warn("Jira API server error: {} on {}", e.getStatusCode(), uri);
            throw new JiraApiException(
                e.getStatusCode().value(),
                "Jira indisponível: " + e.getStatusCode(),
                e
            );
        } catch (Exception e) {
            log.warn("Jira call failed for {}: {}", uri, e.getMessage());
            throw new JiraApiException(0, "Erro de rede chamando Jira: " + e.getMessage(), e);
        }
    }

    private String basicAuthHeader() {
        // Trim defensivo — pega whitespace invisível por copy-paste no YAML.
        String email = props.email() == null ? "" : props.email().trim();
        String token = props.token() == null ? "" : props.token().trim();

        if (log.isDebugEnabled()) {
            log.debug("Building basic auth: email='{}' (len={}) tokenPrefix='{}...' tokenSuffix='...{}' tokenLen={}",
                email,
                email.length(),
                token.length() > 8 ? token.substring(0, 8) : token,
                token.length() > 8 ? token.substring(token.length() - 8) : "",
                token.length());
        }

        String creds = email + ":" + token;
        String encoded = Base64.getEncoder()
            .encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private static String extractMessage(String body) {
        if (body == null || body.isBlank()) return "(sem corpo)";
        // Tenta extrair errorMessages[0]; senão devolve truncado.
        int idx = body.indexOf("\"errorMessages\"");
        if (idx >= 0) {
            int q1 = body.indexOf('"', body.indexOf('[', idx));
            int q2 = body.indexOf('"', q1 + 1);
            if (q1 > 0 && q2 > q1) return body.substring(q1 + 1, q2);
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new JiraApiException(0,
                "Jira não configurado. Defina jira.url/email/token em application-local.yml ou env vars.",
                null);
        }
    }
}
