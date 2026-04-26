package com.hefesto.jira.dto;

import java.util.List;

public record IssueDto(
    String key,
    String summary,
    JiraStatusDto status,
    /** Tipo da issue: "Bug", "Story", "Task" etc. */
    String issueType,
    /** Prioridade: "Highest", "High", "Medium", "Low", "Lowest" — pode ser null. */
    String priority,
    JiraUserDto assignee,
    JiraUserDto reporter,
    /** Markdown a partir do ADF da descrição. Pode ser null se vazio. */
    String description,
    /** Lista textual de critérios extraídos da descrição (heurística simples). */
    List<String> acceptanceCriteria,
    List<CommentDto> comments,
    List<String> labels,
    String sprint,
    long created,
    long updated,
    /** URL pública da issue na UI do Jira. */
    String url
) {}
