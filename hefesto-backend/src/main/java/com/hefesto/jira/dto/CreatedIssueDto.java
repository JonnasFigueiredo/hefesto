package com.hefesto.jira.dto;

/**
 * Resultado da criação de uma issue no Jira.
 *
 * @param key issue key gerada (ex: "PROJ-123").
 * @param id  id numérico interno.
 * @param url link navegável (/browse/KEY).
 */
public record CreatedIssueDto(String key, String id, String url) {}
