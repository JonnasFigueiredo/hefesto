package com.hefesto.jira;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hefesto.jira.dto.CommentDto;
import com.hefesto.jira.dto.IssueDto;
import com.hefesto.jira.dto.IssueListDto;
import com.hefesto.jira.dto.JiraStatusDto;
import com.hefesto.jira.dto.JiraUserDto;

@Service
public class JiraService {

    private static final Logger log = LoggerFactory.getLogger(JiraService.class);

    private final JiraClient client;
    private final JiraProperties props;

    public JiraService(JiraClient client, JiraProperties props) {
        this.client = client;
        this.props = props;
    }

    public boolean isConfigured() {
        return client.isConfigured();
    }

    public IssueListDto.Page search(String jql, String nextPageToken, int maxResults) {
        JsonNode response = client.searchIssues(jql, nextPageToken, maxResults);

        List<IssueListDto> items = new ArrayList<>();
        JsonNode issues = response.path("issues");
        if (issues.isArray()) {
            for (JsonNode issue : issues) {
                items.add(toListItem(issue));
            }
        }

        // O endpoint novo (search/jql) não retorna `total`. Reportamos o que
        // veio nesta página e expomos `nextPageToken` + `isLast` pra paginar.
        boolean isLast = response.path("isLast").asBoolean(true);
        String nextToken = response.path("nextPageToken").asText(null);

        return new IssueListDto.Page(items, items.size(), nextToken, isLast, maxResults);
    }

    public IssueDto getIssue(String key) {
        JsonNode issue = client.getIssue(key);
        JsonNode fields = issue.path("fields");

        JsonNode descriptionAdf = fields.path("description");
        String description = AdfToMarkdown.convert(descriptionAdf);
        if (description.isBlank()) description = null;

        // Comentários: tenta usar fields.comment.comments antes; senão chama endpoint.
        List<CommentDto> comments = parseComments(fields.path("comment").path("comments"));
        if (comments.isEmpty()) {
            try {
                JsonNode standalone = client.getComments(key);
                comments = parseComments(standalone.path("comments"));
            } catch (Exception e) {
                log.debug("Failed to fetch comments separately for {}: {}", key, e.getMessage());
            }
        }

        List<String> acceptance = extractAcceptanceCriteria(description);
        List<String> labels = parseStringArray(fields.path("labels"));
        String sprint = extractSprintName(fields);

        return new IssueDto(
            issue.path("key").asText(),
            fields.path("summary").asText(""),
            parseStatus(fields.path("status")),
            fields.path("issuetype").path("name").asText(null),
            fields.path("priority").path("name").asText(null),
            parseUser(fields.path("assignee")),
            parseUser(fields.path("reporter")),
            description,
            acceptance,
            comments,
            labels,
            sprint,
            parseTimestamp(fields.path("created").asText(null)),
            parseTimestamp(fields.path("updated").asText(null)),
            buildIssueUrl(issue.path("key").asText())
        );
    }

    public List<CommentDto> getComments(String key) {
        JsonNode response = client.getComments(key);
        return parseComments(response.path("comments"));
    }

    /** Pra usar como health check/test connection no Settings. */
    public JiraUserDto getCurrentUser() {
        return parseUser(client.getCurrentUser());
    }

    // ---------- mapping helpers ----------

    private IssueListDto toListItem(JsonNode issue) {
        JsonNode fields = issue.path("fields");
        return new IssueListDto(
            issue.path("key").asText(),
            fields.path("summary").asText(""),
            parseStatus(fields.path("status")),
            fields.path("issuetype").path("name").asText(null),
            fields.path("priority").path("name").asText(null),
            parseUser(fields.path("assignee")),
            parseTimestamp(fields.path("updated").asText(null)),
            buildIssueUrl(issue.path("key").asText())
        );
    }

    private JiraStatusDto parseStatus(JsonNode statusNode) {
        if (statusNode.isMissingNode() || statusNode.isNull()) return null;
        String name = statusNode.path("name").asText("");
        String category = statusNode.path("statusCategory").path("key").asText("new");
        return new JiraStatusDto(name, category);
    }

    private JiraUserDto parseUser(JsonNode userNode) {
        if (userNode.isMissingNode() || userNode.isNull()) return null;
        if (userNode.has("accountId") || userNode.has("displayName")) {
            String avatar = userNode.path("avatarUrls").path("48x48").asText(null);
            return new JiraUserDto(
                userNode.path("accountId").asText(null),
                userNode.path("displayName").asText("Unknown"),
                userNode.path("emailAddress").asText(null),
                avatar
            );
        }
        return null;
    }

    private List<CommentDto> parseComments(JsonNode commentsArray) {
        if (!commentsArray.isArray()) return Collections.emptyList();
        List<CommentDto> result = new ArrayList<>();
        for (JsonNode c : commentsArray) {
            String body = AdfToMarkdown.convert(c.path("body"));
            result.add(new CommentDto(
                c.path("id").asText(""),
                parseUser(c.path("author")),
                body,
                parseTimestamp(c.path("created").asText(null)),
                parseTimestamp(c.path("updated").asText(null))
            ));
        }
        return result;
    }

    private List<String> parseStringArray(JsonNode arr) {
        if (!arr.isArray()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        arr.forEach(n -> out.add(n.asText("")));
        return out;
    }

    private long parseTimestamp(String iso) {
        if (iso == null || iso.isBlank()) return 0L;
        try {
            return OffsetDateTime.parse(iso).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0L;
        }
    }

    /**
     * Extrai linhas que parecem critérios de aceite. Heurística: procura por
     * uma seção que comece com "Critérios de aceite", "Acceptance criteria"
     * ou similar (case-insensitive) e coleta os bullets/linhas que vêm logo
     * abaixo até a próxima seção em branco/heading.
     */
    private List<String> extractAcceptanceCriteria(String description) {
        if (description == null || description.isBlank()) return Collections.emptyList();
        String[] lines = description.split("\\r?\\n");
        List<String> criteria = new ArrayList<>();
        boolean inSection = false;
        for (String raw : lines) {
            String line = raw.trim();
            String lower = line.toLowerCase();
            if (!inSection) {
                if (lower.matches("^#+\\s*(crit[eé]rios?\\s+de\\s+aceit[ae]ç?ão|acceptance\\s+criteria).*")
                    || lower.matches("^\\*?\\*?(crit[eé]rios?\\s+de\\s+aceit[ae]ç?ão|acceptance\\s+criteria).*")) {
                    inSection = true;
                }
                continue;
            }
            if (line.startsWith("#")) break; // próxima seção
            if (line.isEmpty() && !criteria.isEmpty()) break; // bloco terminado
            if (line.startsWith("- ") || line.startsWith("* ")) {
                criteria.add(line.substring(2).strip());
            } else if (line.matches("^\\d+\\.\\s.+")) {
                criteria.add(line.replaceFirst("^\\d+\\.\\s", "").strip());
            }
        }
        return criteria;
    }

    /**
     * Tenta extrair o nome do sprint ativo. Custom field do Jira que vem em
     * fields.sprint, fields.customfield_10020 (legado) etc. Retorna null se
     * não encontrar.
     */
    private String extractSprintName(JsonNode fields) {
        // tenta vários paths
        for (String key : new String[]{"sprint", "customfield_10020", "customfield_10010"}) {
            JsonNode node = fields.path(key);
            if (node.isArray() && node.size() > 0) {
                JsonNode first = node.get(0);
                if (first.isObject() && first.has("name")) return first.path("name").asText(null);
            }
            if (node.isObject() && node.has("name")) return node.path("name").asText(null);
        }
        return null;
    }

    private String buildIssueUrl(String key) {
        if (key == null || key.isBlank() || props.url() == null || props.url().isBlank()) return null;
        String base = props.url().endsWith("/") ? props.url().substring(0, props.url().length() - 1) : props.url();
        return base + "/browse/" + key;
    }
}
