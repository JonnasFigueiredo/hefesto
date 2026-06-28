package com.hefesto.jira.dto;

import java.util.List;

/**
 * Corpo do {@code POST /api/jira/issues} — criação de história/tarefa pela UI.
 *
 * @param projectKey         KEY do projeto (ex: "PROJ"). Obrigatório.
 * @param summary            título da issue. Obrigatório.
 * @param description        descrição em texto livre. Pode ser null.
 * @param acceptanceCriteria critérios de aceite (viram bullet list). Pode ser null.
 * @param issueType          tipo (ex: "Story", "Task"). Default "Story" se null/vazio.
 */
public record CreateIssueRequest(
    String projectKey,
    String summary,
    String description,
    List<String> acceptanceCriteria,
    String issueType
) {}
