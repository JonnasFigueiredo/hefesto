package com.hefesto.jira;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hefesto.jira.dto.AiActionRequests;
import com.hefesto.jira.dto.CommentDto;
import com.hefesto.jira.dto.CreateIssueRequest;
import com.hefesto.jira.dto.CreatedIssueDto;
import com.hefesto.jira.dto.IssueDto;
import com.hefesto.jira.dto.IssueListDto;
import com.hefesto.story.StoryDraftRequest;
import com.hefesto.story.StoryDraftService;

@RestController
@RequestMapping("/api/jira")
public class JiraController {

    private static final Logger log = LoggerFactory.getLogger(JiraController.class);

    private final JiraService service;
    private final JiraProperties props;
    private final StoryDraftService storyDrafts;
    private final com.hefesto.story.ImageStoryDraftService imageStoryDrafts;
    private final com.hefesto.mcp.WorkflowMcpTools workflow;

    public JiraController(JiraService service, JiraProperties props,
                          StoryDraftService storyDrafts,
                          com.hefesto.story.ImageStoryDraftService imageStoryDrafts,
                          com.hefesto.mcp.WorkflowMcpTools workflow) {
        this.service = service;
        this.props = props;
        this.storyDrafts = storyDrafts;
        this.imageStoryDrafts = imageStoryDrafts;
        this.workflow = workflow;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "configured", service.isConfigured()
        );
    }

    /**
     * Endpoint diagnóstico — retorna o que está carregado em memória (tokens
     * mascarados). Use pra confirmar que application-local.yml foi lido
     * corretamente e que o restart do Spring pegou a config nova.
     */
    @GetMapping("/debug")
    public Map<String, Object> debug() {
        String url = props.url() == null ? "" : props.url();
        String email = props.email() == null ? "" : props.email();
        String token = props.token() == null ? "" : props.token();

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("url", url);
        body.put("urlLen", url.length());
        body.put("urlHasTrailingSlash", url.endsWith("/"));
        body.put("email", email);
        body.put("emailLen", email.length());
        body.put("emailTrimmed", email.length() != email.trim().length());
        body.put("tokenPrefix", token.length() > 8 ? token.substring(0, 8) : token);
        body.put("tokenSuffix", token.length() > 8 ? token.substring(token.length() - 8) : "");
        body.put("tokenLen", token.length());
        body.put("tokenTrimmed", token.length() != token.trim().length());
        body.put("tokenHasNewline", token.contains("\n") || token.contains("\r"));
        body.put("tokenHasSpace", token.contains(" "));
        body.put("configured", service.isConfigured());
        return body;
    }

    @GetMapping("/myself")
    public ResponseEntity<?> myself() {
        if (!service.isConfigured()) return notConfigured();
        return ResponseEntity.ok(service.getCurrentUser());
    }

    @GetMapping("/issues")
    public ResponseEntity<?> search(
        @RequestParam(value = "jql", required = false, defaultValue = "") String jql,
        @RequestParam(value = "nextPageToken", required = false) String nextPageToken,
        @RequestParam(value = "maxResults", required = false, defaultValue = "20") int maxResults
    ) {
        if (!service.isConfigured()) return notConfigured();
        IssueListDto.Page page = service.search(jql, nextPageToken, Math.min(maxResults, 100));
        return ResponseEntity.ok(page);
    }

    @GetMapping("/issues/{key}")
    public ResponseEntity<?> issue(@PathVariable String key) {
        if (!service.isConfigured()) return notConfigured();
        IssueDto dto = service.getIssue(key);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/issues/{key}/comments")
    public ResponseEntity<?> comments(@PathVariable String key) {
        if (!service.isConfigured()) return notConfigured();
        List<CommentDto> list = service.getComments(key);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/projects")
    public ResponseEntity<?> projects() {
        if (!service.isConfigured()) return notConfigured();
        return ResponseEntity.ok(service.listProjects());
    }

    @GetMapping("/projects/{key}/issuetypes")
    public ResponseEntity<?> issueTypes(@PathVariable String key) {
        if (!service.isConfigured()) return notConfigured();
        return ResponseEntity.ok(service.listCreatableIssueTypes(key));
    }

    /** Gera um rascunho de história com IA a partir de requisitos/contexto. */
    @PostMapping("/ai/draft-story")
    public ResponseEntity<?> draftStory(@RequestBody StoryDraftRequest req) {
        return ResponseEntity.ok(storyDrafts.draft(req));
    }

    /** Gera um rascunho de história a partir de uma imagem de tela/design. */
    @PostMapping(value = "/ai/draft-story-from-image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> draftFromImage(
            @org.springframework.web.bind.annotation.RequestParam("image") org.springframework.web.multipart.MultipartFile image,
            @org.springframework.web.bind.annotation.RequestParam(value = "context", required = false) String context
    ) throws java.io.IOException {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "no_image", "message", "Envie uma imagem."));
        }
        String name = image.getOriginalFilename() == null ? "" : image.getOriginalFilename();
        String suffix = name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".png";
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("hefesto-design-", suffix);
        try {
            image.transferTo(tmp);
            return ResponseEntity.ok(imageStoryDrafts.draftFromImage(tmp, context));
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }

    /** Avalia a prontidão (INVEST) de uma história; opcionalmente comenta no Jira. */
    @PostMapping("/ai/review-story")
    public ResponseEntity<?> reviewStory(@RequestBody AiActionRequests.ReviewStoryRequest req) {
        if (!service.isConfigured()) return notConfigured();
        return ResponseEntity.ok(
            workflow.reviewStory(req.model(), req.jiraKey(), req.postComment()));
    }

    /** Gera os casos de teste da história e cria uma subtarefa por caso. */
    @PostMapping("/ai/test-subtasks")
    public ResponseEntity<?> testSubtasks(@RequestBody AiActionRequests.TestSubtasksRequest req) {
        if (!service.isConfigured()) return notConfigured();
        return ResponseEntity.ok(
            workflow.createTestSubtasks(req.model(), req.parentKey(), req.context()));
    }

    /** Cria uma história/tarefa. Default de tipo "Story". */
    @PostMapping("/issues")
    public ResponseEntity<?> create(@RequestBody CreateIssueRequest req) {
        if (!service.isConfigured()) return notConfigured();
        String type = (req.issueType() == null || req.issueType().isBlank()) ? "Story" : req.issueType();
        CreatedIssueDto created = service.createStory(
            req.projectKey(), type, req.summary(), req.description(), req.acceptanceCriteria());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @ExceptionHandler(JiraApiException.class)
    public ResponseEntity<Map<String, Object>> handleJiraError(JiraApiException e) {
        log.warn("Jira API error: status={} message={}", e.getHttpStatus(), e.getMessage());
        HttpStatus mapped = e.isUnauthorized() ? HttpStatus.UNAUTHORIZED
            : e.isNotFound() ? HttpStatus.NOT_FOUND
            : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(mapped).body(Map.of(
            "error", "jira_error",
            "status", e.getHttpStatus(),
            "message", e.getMessage()
        ));
    }

    /**
     * Captura erros não esperados (NPE, mapping, etc.) para LOGAR o stack
     * trace completo no console do backend e retornar mensagem útil ao
     * frontend em vez do "500 Internal Server Error" genérico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unexpected error in JiraController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", "unexpected",
            "type", e.getClass().getSimpleName(),
            "message", e.getMessage() == null ? "(no message)" : e.getMessage()
        ));
    }

    private ResponseEntity<Map<String, Object>> notConfigured() {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Map.of(
            "error", "jira_not_configured",
            "message", "Configure jira.url, jira.email e jira.token em application-local.yml ou via env vars."
        ));
    }
}
