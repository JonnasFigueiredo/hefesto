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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hefesto.jira.dto.CommentDto;
import com.hefesto.jira.dto.IssueDto;
import com.hefesto.jira.dto.IssueListDto;

@RestController
@RequestMapping("/api/jira")
public class JiraController {

    private static final Logger log = LoggerFactory.getLogger(JiraController.class);

    private final JiraService service;

    public JiraController(JiraService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "configured", service.isConfigured()
        );
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
